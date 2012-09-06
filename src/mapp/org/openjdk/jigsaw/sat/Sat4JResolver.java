/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jigsaw.sat;

import java.lang.module.Dependence.Modifier;
import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleSystem;
import java.lang.module.ModuleView;
import java.lang.module.ViewDependence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openjdk.jigsaw.JigsawModuleSystem;
import org.openjdk.jigsaw.Library;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.StringNegator;
import org.sat4j.pb.tools.WeightedObject;

// ## Change so not mapping to module id strings
public class Sat4JResolver implements Resolver {

    private final ModuleSystem ms = JigsawModuleSystem.instance();

    private final Library l;

    public Sat4JResolver(Library l) {
        this.l = l;
    }

    @Override
    public ResolverResult resolve(ModuleIdQuery... midqs) throws ResolverException {
        try {
            return _resolve(midqs);
        } catch (ResolverException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ResolverException(ex);
        }
    }

    private ResolverResult _resolve(ModuleIdQuery... midqs) throws Exception {
        ModuleGraphTraverser t = new ModuleGraphTraverser(l);
        ReifiedDependencies rds = new ReifiedDependencies();
        t.traverse(rds, midqs);

        IPBSolver s = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
        s.setVerbose(true);

        DependencyHelper<String, String> helper = new DependencyHelper<>(s);
        helper.setNegator(StringNegator.instance);

        Map<String, String> viewOrAliasNameToModuleName = getViewOrAliasNameToModuleNameMap(rds);
        Map<ModuleId, ModuleId> viewOrAliasIdToModuleId = new HashMap<>();
        Set<String> optionals = new HashSet<>();
        Map<ModuleId, Set<ModuleId>> notPermitted = new HashMap<>();

        // Module dependencies
        for (ModuleId rmid : rds.modules) {
            ModuleInfo rmi = rds.idToView.get(rmid).moduleInfo();
            for (ViewDependence vd : rmi.requiresModules()) {
                Set<ModuleId> mids = rds.dependenceToMatchingIds.get(vd);
                if (!mids.isEmpty()) {
                    // Process views and aliases
                    for (ModuleId mid : mids) {
                        ModuleView mv = rds.idToView.get(mid);
                        ModuleInfo mi = mv.moduleInfo();

                        if (!mi.id().equals(mid)) {
                            // View or alias to module
                            // ## distinguish between view or alias?
                            viewOrAliasIdToModuleId.put(mid, mi.id());

                        }

                        if (!mv.permits().isEmpty()) {
                            if (!mv.permits().contains(rmi.id().name())) {
                                Set<ModuleId> npmids = notPermitted.get(mv.id());
                                if (npmids == null) {
                                    npmids = new HashSet<>();
                                    notPermitted.put(mv.id(), npmids);
                                }
                                npmids.add(rmi.id());
                            }
                        }
                    }

                    List<String> names = new ArrayList<>(mids.size());
                    for (ModuleId mid : mids) {
                        names.add(mid.toString());
                    }

                    // Optional dependence
                    // Add the literal "*" + mid to represent absense
                    if (vd.modifiers().contains(Modifier.OPTIONAL)) {
                        String moduleName = viewOrAliasNameToModuleName.get(vd.query().name());
                        optionals.add(moduleName);
                        names.add("*" + moduleName);
                    }

                    // (~rmid v mid1 v mid1 v mid3 v ...)
                    helper.disjunction(rmid.toString()).
                            implies(names.toArray(new String[0])).
                            named(String.format("Module %s depends on %s", rmid, names.toString()));
                } else {
                    String moduleName = viewOrAliasNameToModuleName.get(vd.query().name());
                    if (moduleName != null) {
                        // 1 or more modules are present but those do not match the query

                        if (vd.modifiers().contains(Modifier.OPTIONAL)) {
                            helper.disjunction(rmid.toString()).
                                    implies("*" + moduleName).
                                    named(String.format("Module %s has an optional dependency %s, which matches no modules", rmid, vd.query()));
                            optionals.add(moduleName);
                        } else {
                            // Fail with explicit conflicting clauses
                            helper.disjunction(rmid.toString()).
                                    implies("*" + moduleName).
                                    named(String.format("Module %s has an dependency %s, which matches no modules", rmid, vd.query()));
                            helper.disjunction(rmid.toString()).
                                    implies("-" + "*" + moduleName).
                                    named(String.format("Module %s cannot be installed", rmid, vd.query()));
                        }
                    } else {
                        // No modules match

                        // ## Cannot distinguish between no modules in the module
                        // library or no modules in the dependency graph
                        // The former can occur if all queries fail to match
                        if (vd.modifiers().contains(Modifier.OPTIONAL)) {
                            helper.disjunction(rmid.toString()).
                                    implies("*" + vd.query().name()).
                                    named(String.format("Module %s has an optional dependency %s, which matches no modules", rmid, vd.query()));
                        } else {
                            // Fail with explicit conflicting clauses
                            helper.disjunction(rmid.toString()).
                                    implies("*" + vd.query().name()).
                                    named(String.format("Module %s has an dependency %s, which matches no modules", rmid, vd.query()));
                            helper.disjunction(rmid.toString()).
                                    implies("-" + "*" + vd.query().name()).
                                    named(String.format("Module %s cannot be installed", rmid, vd.query()));
                        }
                    }
                }
            }
        }

        // Only one version of a module
        // Collapse to module names
        Set<String> midNames = new HashSet<>();
        for (ModuleId mid : rds.modules) {
            midNames.add(mid.name());
        }
        for (String midName : midNames) {            
            Set<ModuleId> versions = rds.nameToIds.get(midName);

            if (versions.size() > 1 || (versions.size() > 0 && optionals.contains(midName))) {
                List<String> names = new ArrayList<>(versions.size());
                for (ModuleId mid : versions) {
                    names.add("-" + mid.toString());
                }

                // There is at least one optional dependence on the module
                if (optionals.contains(midName)) {
                    names.add("-" + "*" + midName);
                }

                helper.atLeast(String.format("Only one version of module %s", midName),
                        names.size() - 1,
                        names.toArray(new String[0]));
            }
        }

        // Root modules to be installed
        for (Map.Entry<ModuleIdQuery, Set<ModuleId>> e : rds.roots.entrySet()) {
            ModuleIdQuery midq = e.getKey();
            Set<ModuleId> versions = e.getValue();

            if (!versions.isEmpty()) {
                // Process views and aliases
                for (ModuleId mid : versions) {
                    ModuleInfo mi = rds.idToView.get(mid).moduleInfo();

                    if (!mi.id().equals(mid)) {
                        // View or alias to module
                        // ## distinguish between view or alias?
                        viewOrAliasIdToModuleId.put(mid, mi.id());
                    }
                }

                List<String> names = new ArrayList<>(versions.size());
                for (ModuleId mid : versions) {
                    names.add(mid.toString());
                }

                // (mid1 v mid1 v mid3 v ...)
                helper.clause(
                        String.format("Module in query %s to be installed", midq),
                        names.toArray(new String[0]));
            } else {
                // Fail with explicit conflicting clauses
                helper.clause(String.format("Root dependency %s matches no modules", midq), midq.name());
                helper.clause(String.format("Root dependency %s failed to resolve", midq), "-" + midq.name());
            }
        }

        // Not permitted
        for (Map.Entry<ModuleId, Set<ModuleId>> e : notPermitted.entrySet()) {
            ModuleId mvid = e.getKey();
            Set<ModuleId> mids = e.getValue();

            for (ModuleId mid : mids) {
                helper.clause(String.format("Module %s is not permitted to depend on %s", mid, mvid), "-" + mvid, "-" + mid);
            }
        }

        // View and aliases
        for (Map.Entry<ModuleId, ModuleId> e : viewOrAliasIdToModuleId.entrySet()) {
            // view/alias id => module id
            helper.disjunction(e.getKey().toString()).
                    implies(e.getValue().toString()).
                    named(String.format("%s is a view or alias of module %s", e.getKey(), e.getValue()));
        }


        // Objective function
        // Optimize to prefer newer to older versions
        // ## Make configurable based on phase e.g. compile, install, runtime
        {
            List<String> names = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();

            for (Map.Entry<String, Set<ModuleId>> e : rds.nameToIds.entrySet()) {
                String name = e.getKey();
                Set<ModuleId> mids = e.getValue();

                int w = mids.size();
                if (optionals.contains(name)) {
                    // Literal for optional dependence
                    names.add("*" + name);
                    weights.add(w + 1);
                }
                for (ModuleId mid : mids) {
                    names.add(mid.toString());
                    weights.add(w--);
                }
            }

            WeightedObject<String>[] wos = new WeightedObject[names.size()];
            for (int i = 0; i < names.size(); i++) {
                wos[i] = WeightedObject.newWO(names.get(i), weights.get(i));
            }
            helper.setObjectiveFunction(wos);
        }

        if (helper.hasASolution()) {
            Set<String> names = new LinkedHashSet<>(helper.getASolution());
            final Set<ModuleId> mids = new LinkedHashSet<>();

            // Preserve topological order of solution
            for (ModuleId mid : rds.modules) {
                if (names.contains(mid.toString())) {
                    // Ignore +v literal corresponding to view/aliase or optional dependence
                    mids.add(mid);
                }
            }

            return new ResolverResult() {
                @Override
                public Set<ModuleId> resolvedModuleIds() {
                    return Collections.unmodifiableSet(mids);
                }
                
                @Override
                public String toString() {
                    return mids.toString();
                }
            };
        } else {
            // ## Produce meaningful structure that can be processed by javac
            Set<String> why = helper.why();
            throw new ResolverException(why.toString());
        }
    }

    Map<String, String> getViewOrAliasNameToModuleNameMap(ReifiedDependencies rds) {
        Map<String, String> names = new HashMap<>();
        for (ModuleId mid : rds.modules) {
            ModuleInfo mi = rds.idToView.get(mid).moduleInfo();

            names.put(mid.name(), mid.name());

            for (ModuleView mv : mi.views()) {
                names.put(mv.id().name(), mid.name());

                for (ModuleId aliasMid : mv.aliases()) {
                    names.put(aliasMid.name(), mid.name());
                }
            }
        }

        return names;
    }
}
