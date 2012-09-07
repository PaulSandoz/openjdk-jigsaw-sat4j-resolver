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
package org.openjdk.jigsaw.test.sat;

import java.io.File;
import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleSystem;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openjdk.jigsaw.JigsawModuleSystem;
import org.openjdk.jigsaw.Library;
import org.openjdk.jigsaw.SimpleLibrary;
import org.openjdk.jigsaw.sat.Sat4JResolver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class JDKResolverTest {

    ModuleSystem ms = JigsawModuleSystem.instance();

    Library mlib;

    Sat4JResolver r;

    public JDKResolverTest() throws Exception {
        mlib = SimpleLibrary.open(new File(System.getProperty("test.library")));
    }

    @BeforeMethod
    void before() {
        r = new Sat4JResolver(mlib);
    }

    @DataProvider(name = "roots")
    public Object[][] createRoots() {
        return new Object[][]{
                    {"jdk.base"},
                    {"jdk.jre"},
                    {"jdk"},
                    {"mtest"},
        };
    }

    @Test(dataProvider = "roots")
    public void testResolver(String root) {
        Set<ModuleId> mids = r.resolve(queryIds(root)).resolvedModuleIds();
        System.out.println(mids);
    }

    protected Collection<ModuleIdQuery> queryIds(String... midqNames) {
        Set<ModuleIdQuery> midqs = new LinkedHashSet<>();
        for (String name : midqNames) {
            midqs.add(ms.parseModuleIdQuery(name));
        }
        return midqs;
    }
}
