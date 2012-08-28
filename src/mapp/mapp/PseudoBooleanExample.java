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

import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.OptToPBSATAdapter;
import org.sat4j.pb.PseudoOptDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.StringNegator;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.IVec;

public class PseudoBooleanExample {

    public static void main(String... args) throws Exception {
        IPBSolver s = new OptToPBSATAdapter(new PseudoOptDecorator(SolverFactory.newDefault()));
        s.setVerbose(true);

        DependencyHelper<String, String> helper = new DependencyHelper<>(s);

        helper.setNegator(StringNegator.instance);

        // A1's dependencies
        helper.disjunction("A1").implies("B1", "B2", "B3").
                named("A1 requires B[1,3]");
        helper.disjunction("A1").implies("C1").
                named("A1 requires C1");
        helper.disjunction("A1").implies("D1", "D2", "D3").
                named("A1 requires D[1, 3]");

        // C1's dependencies
        helper.disjunction("C1").implies("B2", "B3", "B4").
                named("C1 requires B[2,4]");
        helper.disjunction("C1").implies("D2", "D3", "D4").
                named("C1 requires D[2,4]");

        // Only one version of B is allowed
        helper.atLeast("Only one version of B", 3, "-B1", "-B2", "-B3", "-B4");
        // Only one version of D is allowed
        helper.atLeast("Only one version of D", 3, "-D1", "-D2", "-D3", "-D4");
        // The above is a less verbose way is declaring pair wise contraints of
        // (-D1 v -D2) ...

        helper.clause("A1 to be installed", "A1");

        // Minimize for smallest version in a range
        // Unique weights only need to be assigned for the same module name.
        // For a totally ordered version scheme the weight of a module can 
        // be it's index in an list sorted using the version as the primary key
        String[] x = new String[]{"B1", "B2", "B3", "B4", "D1", "D2", "D3", "D4"};
        int[] v = new int[]{1, 2, 3, 4, 1, 2, 3, 4};
        WeightedObject<String>[] wos = new WeightedObject[x.length];
        for (int i = 0; i < x.length; i++) {
            wos[i] = WeightedObject.newWO(x[i], v[i]);
        }
        helper.setObjectiveFunction(wos);

        if (helper.hasASolution()) {
            IVec<String> sol = helper.getSolution();
            System.out.println(sol);
        } else {
            System.out.println(helper.why());
        }
    }
}