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
package mapp;

import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleView;
import java.lang.module.ViewDependence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.openjdk.jigsaw.Catalog;

public class ModuleGraphTraverser {

    private final Catalog cat;

    private final Set<ModuleId> visited = new HashSet<>();

    public ModuleGraphTraverser(Catalog cat) {
        this.cat = cat;
    }

    abstract class Node {

        final int depth;

        Node(int depth) {
            this.depth = depth;
        }

        abstract void process(Deque<Node> stack) throws Exception;

        void println(Object o) {
            System.out.println(b(depth, o).toString());
        }

        StringBuilder b(int depth, Object o) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append(" ");
            }
            return sb.append(o);
        }
    }

    class ModuleQueryNode extends Node {

        final ModuleIdQuery moduleIdQuery;

        ModuleQueryNode(int depth, ModuleIdQuery moduleIdQuery) {
            super(depth);
            this.moduleIdQuery = moduleIdQuery;
        }

        void process(Deque<Node> stack) throws Exception {
            String moduleName = moduleIdQuery.name();

            // Find all module id versions of module name
            List<ModuleId> ms = cat.findModuleIds(moduleName);
            Collections.sort(ms);

            for (ModuleId m : ms) {
                if (moduleIdQuery.matches(m)) {
                    process(stack, m);
                } else {
                    // Hack
                    if (moduleName.equals("java.base") && moduleName.equals(m.name())) {
                        process(stack, m);
                    }
                }
            }
        }

        protected void process(Deque<Node> stack, ModuleId m) {
            stack.addFirst(new ModuleNode(depth + 1, m));
        }
    }

    class ModuleNode extends Node {

        final ModuleId m;

        public ModuleNode(int depth, ModuleId m) {
            super(depth);
            this.m = m;
        }

        void process(Deque<Node> stack) throws Exception {
            ModuleInfo moduleInfo = cat.readModuleInfo(m);

            if (visited.contains(moduleInfo.id())) {
                println(m + " -> module " + moduleInfo.id() + " VISITED");
                return;
            }

            visited.add(moduleInfo.id());

            println(m + " -> module " + moduleInfo.id());

            for (ModuleView mv : moduleInfo.views()) {
                StringBuilder sb = new StringBuilder("-> view ").append(mv.id());

                for (ModuleId alias : mv.aliases()) {
                    sb.append(" -> alias ").append(alias);
                }
                println(sb.toString());
            }

            process(moduleInfo);

            // This assumes dependencies are ordered as declared in module-info
            List<ViewDependence> vds = new ArrayList<>(moduleInfo.requiresModules());
            // Preserve declared order on stack
            Collections.reverse(vds);
            for (ViewDependence vd : vds) {
                println(vd);
                process(stack, vd);
            }
        }

        protected void process(ModuleInfo moduleInfo) {
        }

        protected void process(Deque<Node> stack, ViewDependence vd) {
            stack.addFirst(new ViewDependenceNode(depth + 1, m, vd));
        }
    }

    class ViewDependenceNode extends ModuleQueryNode {

        final ModuleId m;

        final ViewDependence vd;

        ViewDependenceNode(int depth, ModuleId m, ViewDependence vd) {
            super(depth, vd.query());
            this.m = m;
            this.vd = vd;
        }

        @Override
        protected void process(Deque<Node> stack, ModuleId dm) {
            stack.addFirst(new ModuleNodeWithViewDependency(depth + 1, m, vd, dm));
        }
    }

    class ModuleNodeWithViewDependency extends ModuleNode {

        final ModuleId rm;

        final ViewDependence vd;

        final ModuleId dm;

        ModuleNodeWithViewDependency(int depth, ModuleId rm, ViewDependence vd, ModuleId dm) {
            super(depth, dm);
            this.rm = rm;
            this.vd = vd;
            this.dm = dm;
        }

        @Override
        protected void process(ModuleInfo moduleInfo) {
            ModuleView moduleView = getModuleView(moduleInfo, dm);

            // TODO validate permits here
            // If requires local goes away there is no need for the ViewDependence
            // rm.id().name() must be a member of moduleView.permits()
        }

        @Override
        protected void process(Deque<Node> stack, ViewDependence vd) {
            stack.addFirst(new ViewDependenceNode(depth + 1, dm, vd));
        }

        ModuleView getModuleView(ModuleInfo moduleInfo, ModuleId moduleId) {
            for (ModuleView moduleView : moduleInfo.views()) {
                if (moduleView.id().equals(moduleId) || moduleView.aliases().contains(moduleId)) {
                    return moduleView;
                }
            }

            return null;
        }
    }

    public void traverse(ModuleIdQuery moduleIdQuery) throws Exception {
        Deque<Node> stack = new LinkedList<>();

        // Depth first search of module dependency graph
        stack.addFirst(new ModuleQueryNode(0, moduleIdQuery));
        while (!stack.isEmpty()) {
            stack.removeFirst().process(stack);
        }
    }
}
