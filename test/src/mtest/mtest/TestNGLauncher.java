/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package mtest;

import org.openjdk.jigsaw.test.sat.JDKResolverTest;
import org.openjdk.jigsaw.test.sat.NoMatchResolverTest;
import org.openjdk.jigsaw.test.sat.OptionalResolverTest;
import org.openjdk.jigsaw.test.sat.PermitResolverTest;
import org.openjdk.jigsaw.test.sat.RequiresResolverTest;
import org.openjdk.jigsaw.test.sat.ServiceDependencesTest;
import org.openjdk.jigsaw.test.sat.ViewAliasResolverTest;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

public class TestNGLauncher {

    public static void main(String[] args) {
        TestListenerAdapter tla = new TestListenerAdapter();
        TestNG testng = new TestNG();
        testng.setTestClasses(new Class[]{
                    RequiresResolverTest.class,
                    ViewAliasResolverTest.class,
                    OptionalResolverTest.class,
                    NoMatchResolverTest.class,
                    PermitResolverTest.class,
                    JDKResolverTest.class,
                    ServiceDependencesTest.class,
                });
        testng.addListener(tla);
        testng.run();
    }
}