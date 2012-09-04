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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.openjdk.jigsaw.Catalog;

/**
 * Traverses the module graph, for a given set of root queries, using a 
 * depth first search and reports, in search order, on module dependencies 
 * for modules present in a catalog.
 *
 */
public class ModuleGraphTraverser {

    // ## Hook up to Jigsaw tracing
    private boolean tracing = false;

    private final Catalog cat;

    public ModuleGraphTraverser(Catalog cat) {
        this.cat = cat;
    }

    static class State {

        final ModuleGraphListener mgl;

        final Set<ModuleId> visited = new HashSet<>();

        final Deque<Node> stack = new LinkedList<>();

        public State(ModuleGraphListener mgl) {
            this.mgl = mgl;
        }

        boolean isVisited(ModuleInfo mi) {
            return !visited.add(mi.id());
        }

        void push(Node n) {
            stack.addFirst(n);
        }

        Node pop() {
            return stack.removeFirst();
        }

        boolean isEmpty() {
            return stack.isEmpty();
        }
    }

    abstract class Node {

        final int depth;

        Node(int depth) {
            this.depth = depth;
        }

        abstract void process(State state) throws Exception;

        void trace(Object o) {
            if (!tracing) {
                return;
            }

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

        final ModuleIdQuery midq;

        ModuleQueryNode(int depth, ModuleIdQuery midq) {
            super(depth);

            this.midq = midq;
        }

        @Override
        void process(State s) throws Exception {
            process(s, midq);
            
            String moduleName = midq.name();

            // Find all module id versions of module name
            List<ModuleId> mids = cat.findModuleIds(moduleName);
            // Sort from lowest to highest version
            Collections.sort(mids);

            for (ModuleId mid : mids) {
                if (midq.matches(mid)) {
                    process(s, mid);
                } else {
                    // Hack
                    if (moduleName.equals("java.base") && moduleName.equals(mid.name())) {
                        process(s, mid);
                    }
                }
            }
        }

        protected void process(State s, ModuleIdQuery midq) {
            s.mgl.onRootDependence(midq);                        
        }
        
        protected void process(State s, ModuleId mid) {
            s.push(new ModuleNode(depth + 1, midq, mid));
        }
    }

    class ModuleNode extends Node {

        final ModuleIdQuery midq;

        final ModuleId mid;

        public ModuleNode(int depth, ModuleIdQuery midq, ModuleId m) {
            super(depth);

            this.midq = midq;
            this.mid = m;
        }

        @Override
        void process(State s) throws Exception {
            ModuleInfo mi = cat.readModuleInfo(mid);

            process(s, mi);

            if (s.isVisited(mi)) {
                if (tracing) {
                    trace(mid + " -> module " + mi.id() + " VISITED");
                }
                return;
            }

            if (tracing) {
                trace(mid + " -> module " + mi.id());

                for (ModuleView mv : mi.views()) {
                    StringBuilder sb = new StringBuilder("-> view ").append(mv.id());

                    for (ModuleId alias : mv.aliases()) {
                        sb.append(" -> alias ").append(alias);
                    }
                    trace(sb.toString());
                }
            }

            // This assumes dependencies are ordered as declared in module-info
            List<ViewDependence> vds = new ArrayList<>(mi.requiresModules());
            // Preserve declared order on stack
            Collections.reverse(vds);
            for (ViewDependence vd : vds) {
                if (tracing) {
                    trace(vd);
                }
                process(s, mi, vd);
            }
        }

        protected void process(State s, ModuleInfo mi) {
            s.mgl.onMatchingRootDependence(midq, getModuleView(mi, mid));
        }

        protected void process(State s, ModuleInfo mi, ViewDependence vd) {
            s.push(new ViewDependenceNode(depth + 1, mi, vd));
        }
    }

    class ViewDependenceNode extends ModuleQueryNode {

        final ModuleInfo rmi;

        final ViewDependence vd;

        ViewDependenceNode(int depth, ModuleInfo rmi, ViewDependence vd) {
            super(depth, vd.query());
            this.rmi = rmi;
            this.vd = vd;
        }

        @Override
        protected void process(State s, ModuleIdQuery midq) {
            s.mgl.onViewDependence(depth, rmi, vd);
        }

        @Override
        protected void process(State s, ModuleId dmid) {
            s.push(new ModuleNodeWithViewDependency(depth + 1, rmi, vd, dmid));
        }
    }

    class ModuleNodeWithViewDependency extends ModuleNode {

        final ModuleInfo rmi;

        final ViewDependence vd;

        final ModuleId dmid;

        ModuleNodeWithViewDependency(int depth, ModuleInfo rmi, ViewDependence vd, ModuleId dmid) {
            super(depth, vd.query(), dmid);

            this.rmi = rmi;
            this.vd = vd;
            this.dmid = dmid;
        }

        @Override
        protected void process(State s, ModuleInfo mi) {
            s.mgl.onMatchingViewDependence(depth, rmi, vd, getModuleView(mi, dmid));
        }

        @Override
        protected void process(State s, ModuleInfo mi, ViewDependence vd) {
            s.push(new ViewDependenceNode(depth + 1, mi, vd));
        }
    }

    ModuleView getModuleView(ModuleInfo mi, ModuleId mid) {
        for (ModuleView mv : mi.views()) {
            if (mv.id().equals(mid) || mv.aliases().contains(mid)) {
                return mv;
            }
        }

        return null;
    }

    public void traverse(ModuleGraphListener mgl, ModuleIdQuery... midqs) throws ModuleGraphTraversalException {
        Objects.requireNonNull(mgl);
        Objects.requireNonNull(midqs);

        State s = new State(mgl);

        // Add roots, in order of declaration
        for (ModuleIdQuery midq : midqs) {
            s.stack.addLast(new ModuleQueryNode(0, midq));
        }
        // Depth first search of module dependency graph
        try {
            while (!s.isEmpty()) {
                s.pop().process(s);
            }
        } catch (ModuleGraphTraversalException e) {
            throw e;
        } catch (Exception e) {
            throw new ModuleGraphTraversalException(e);
        }
    }
}