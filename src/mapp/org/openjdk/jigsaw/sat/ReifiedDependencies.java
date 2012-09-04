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
import java.lang.module.ModuleView;
import java.lang.module.ViewDependence;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A mutable set of reified dependencies.
 * <p>
 * Such dependencies can be produced from traversing the module dependency
 * graph for a given catalog.
 */
public class ReifiedDependencies implements ModuleGraphListener {

    // Map of module view/alias id to module view
    // In topological order of dependency graph traversal (depth first search)
    // The key set contains all known modules view ids
    public final Map<ModuleId, ModuleView> idToView;

    // Module ids grouped by module name
    // The module ids are sorted by version, from least to greatest
    public final Map<String, Set<ModuleId>> nameToIds;

    // Root module view/alias ids, grouped by module id query name
    // Keys are in declaration order
    // The module view/alias ids are sorted by version, from least to greatest
    public final Map<ModuleIdQuery, Set<ModuleId>> roots;

    // The set of module ids
    // In topological order of dependency graph traversal (depth first search)
    public final Set<ModuleId> modules;

    // View dependence to matching module view/alias ids
    // Order of keys is unspecified
    // The module view/alias ids are sorted by version, from least to greatest
    public final Map<ViewDependence, Set<ModuleId>> dependenceToMatchingIds;

    public ReifiedDependencies() {
        idToView = new LinkedHashMap<>();
        nameToIds = new HashMap<>();
        roots = new LinkedHashMap<>();
        modules = new LinkedHashSet<>();
        dependenceToMatchingIds = new HashMap<>();
    }

    private static final Comparator<ModuleId> MODULE_ID_COMPARATOR = new Comparator<ModuleId>() {
        @Override
        public int compare(ModuleId o1, ModuleId o2) {
            return o1.compareTo(o2);
        }
    };

    @Override
    public void onRootDependence(ModuleIdQuery midq) {
        // Create empty set for query to detect when there are no matches
        if (roots.get(midq) == null) {
            roots.put(midq, Collections.EMPTY_SET);
        }
    }

    @Override
    public void onMatchingRootDependence(ModuleIdQuery midq, ModuleId mid, ModuleView mv) {
        Set<ModuleId> mvs = roots.get(midq);
        if (mvs.isEmpty()) {
            mvs = new TreeSet<>(MODULE_ID_COMPARATOR);
            roots.put(midq, mvs);
        }
        mvs.add(mv.id());

        // ## Aliases are ignored
        
        onModuleView(mv);
    }

    @Override
    public void onViewDependence(int depth, ModuleInfo rmi, ViewDependence vd) {
        // Create empty set for view dependences to detect when there are no matches
        if (dependenceToMatchingIds.get(vd) == null) {
            dependenceToMatchingIds.put(vd, Collections.EMPTY_SET);
        }
    }
    
    @Override
    public void onMatchingViewDependence(int depth, ModuleInfo rmi, ViewDependence vd, ModuleId mid, ModuleView mv) {
        Set<ModuleId> mvs = dependenceToMatchingIds.get(vd);
        if (mvs.isEmpty()) {
            mvs = new LinkedHashSet<>();
            dependenceToMatchingIds.put(vd, mvs);
        }
        mvs.add(mv.id());

        // ## Aliases are ignored
        
        onModuleView(mv);
    }

    private void onModuleView(ModuleView mv) {
        onModuleInfo(mv.moduleInfo());

        if (!idToView.containsKey(mv.id())) {
            idToView.put(mv.id(), mv);
        }        
    }
    
    private void onModuleInfo(ModuleInfo mi) {
        final ModuleId mid = mi.id();

        modules.add(mid);
        
        Set<ModuleId> mvs = nameToIds.get(mid.name());
        if (mvs == null) {
            mvs = new TreeSet<>(MODULE_ID_COMPARATOR);
            nameToIds.put(mid.name(), mvs);
        }
        mvs.add(mid);

        if (!idToView.containsKey(mid)) {
            idToView.put(mid, mi.defaultView());
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<ModuleIdQuery, Set<ModuleId>> e : roots.entrySet()) {
            sb.append(e.getKey()).append(" -> ").append(e.getValue());
            sb.append("\n");
        }

        for (ModuleId mid : modules) {
            sb.append(mid).append("\n");

            ModuleView mv = idToView.get(mid);
            for (ViewDependence vd : mv.moduleInfo().requiresModules()) {
                sb.append("  ").append(vd).append(" -> ");

                Set<ModuleId> mids = dependenceToMatchingIds.get(vd);
                if (mids != null) {
                    sb.append(mids.toString());
                } else {
                    sb.append("[]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
