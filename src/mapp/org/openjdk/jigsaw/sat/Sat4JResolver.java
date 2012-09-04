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

import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleSystem;
import java.lang.module.ViewDependence;
import java.util.ArrayList;
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
    public Set<ModuleId> resolve(ModuleIdQuery... midqs) throws ResolverException {
        try {
            return _resolve(midqs);
        } catch (ResolverException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResolverException(ex);
        }
    }

    private Set<ModuleId> _resolve(ModuleIdQuery... midqs) throws Exception {
        ModuleGraphTraverser t = new ModuleGraphTraverser(l);
        ReifiedDependencies rds = new ReifiedDependencies();
        t.traverse(rds, midqs);

        IPBSolver s = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
        s.setVerbose(true);

        DependencyHelper<String, String> helper = new DependencyHelper<>(s);
        helper.setNegator(StringNegator.instance);

        // Module dependencies
        for (ModuleId rmid : rds.modules) {
            ModuleInfo rmi = rds.idToView.get(rmid).moduleInfo();
            for (ViewDependence vd : rmi.requiresModules()) {
                Set<ModuleId> mids = rds.dependenceToMatchingIds.get(vd);
                if (!mids.isEmpty()) {
                    List<String> names = new ArrayList<>(mids.size());
                    for (ModuleId mid : mids) {
                        names.add(mid.toString());
                    }
                    helper.disjunction(rmid.toString()).
                            implies(names.toArray(new String[0])).
                            named(String.format("Module %s depends on %s", rmid.toString(), names.toString()));

                    // ## Views and aliases

                    // ## Permits

                    // ## Optional dependence
                } else {
                    // ## No matching modules for dependence    
                    // ## Optional dependence

                    // ## Is module id query name the name of a non-default view?
                }
            }
        }

        // Only one version of a module
        for (Map.Entry<String, Set<ModuleId>> e : rds.nameToIds.entrySet()) {
            String name = e.getKey();
            Set<ModuleId> versions = e.getValue();

            if (versions.size() > 1) {
                List<String> names = new ArrayList<>(versions.size());
                for (ModuleId mid : versions) {
                    names.add("-" + mid.toString());
                }

                helper.atLeast(String.format("Only one version of module %s", name),
                        versions.size() - 1,
                        names.toArray(new String[0]));
            }
        }

        // Root modules to be installed
        for (Map.Entry<ModuleIdQuery, Set<ModuleId>> e : rds.roots.entrySet()) {
            ModuleIdQuery name = e.getKey();
            Set<ModuleId> versions = e.getValue();

            if (!versions.isEmpty()) {
                List<String> names = new ArrayList<>(versions.size());
                for (ModuleId mid : versions) {
                    names.add(mid.toString());
                }

                helper.clause(
                        String.format("Module %s to be installed", name.toString()),
                        names.toArray(new String[0]));
            } else {
                // ## No matching modules for root query 
                // ## Is module id query name the name of a non-default view?
            }
        }



        // Objective function
        // Optimize to prefer newer to older versions
        // ## Make configurable based on phase e.g. compile, install, runtime
        {
            List<String> names = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            for (Set<ModuleId> e : rds.nameToIds.values()) {
                int w = e.size();
                for (ModuleId mid : e) {
                    names.add(mid.toString());
                    weights.add(w--);
                }

                // ## literal for optional dependence
            }
            WeightedObject<String>[] wos = new WeightedObject[names.size()];
            for (int i = 0; i < names.size(); i++) {
                wos[i] = WeightedObject.newWO(names.get(i), weights.get(i));
            }
            helper.setObjectiveFunction(wos);
        }

        if (helper.hasASolution()) {
            Set<String> names = new LinkedHashSet<>(helper.getASolution());
            Set<ModuleId> mids = new LinkedHashSet<>();

            // Preserve topological order of solution
            for (ModuleId mid : rds.idToView.keySet()) {
                if (names.contains(mid.toString())) {
                    // Transform module view id to module id to remove
                    // views from the solution                    
                    mids.add(rds.idToView.get(mid).moduleInfo().id());
                }
            }

            return mids;
        } else {
            // ## Produce meaningful structure that can be processed by javac
            Set<String> why = helper.why();
            throw new ResolverException(why.toString());
        }
    }
}
