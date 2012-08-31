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

import java.io.File;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleView;
import java.lang.module.ViewDependence;
import org.openjdk.jigsaw.JigsawModuleSystem;
import org.openjdk.jigsaw.Library;
import org.openjdk.jigsaw.SimpleLibrary;
import org.openjdk.jigsaw.sat.ModuleDependencyReifier;
import org.openjdk.jigsaw.sat.ModuleGraphListener;
import org.openjdk.jigsaw.sat.ModuleGraphTraverser;

public class App {

    public static void main(String... args) throws Exception {
        traverse(args[0], args[1]);
//        PseudoBooleanExample.main();
    }

    public static void traverse(String libraryPath, String moduleQuery) throws Exception {
        JigsawModuleSystem jms = JigsawModuleSystem.instance();

        Library l = SimpleLibrary.open(new File(libraryPath));

        ModuleIdQuery rootQuery = jms.parseModuleIdQuery(moduleQuery);

        ModuleGraphTraverser t = new ModuleGraphTraverser(l);
        t.traverse(new ModuleGraphListener() {
            @Override
            public void onRootModuleDependency(ModuleIdQuery mq, ModuleView mv) {
                System.out.println(mq + " -> " + mv.id() + " [" + mv.moduleInfo().id() + "]");
            }

            @Override
            public void onModuleDependency(int depth, ModuleInfo rmi, ViewDependence vd, ModuleView mv) {
                for (int i = 0; i < depth; i++) {
                    System.out.print(" ");
                }
                System.out.println(rmi.id() + " -> " + vd + " -> " + mv.id() + " [" + mv.moduleInfo().id() + "]");
            }
        }, rootQuery);
        
        ModuleDependencyReifier x = new ModuleDependencyReifier();
        t.traverse(x, rootQuery);
        System.out.println(x);
    }
}