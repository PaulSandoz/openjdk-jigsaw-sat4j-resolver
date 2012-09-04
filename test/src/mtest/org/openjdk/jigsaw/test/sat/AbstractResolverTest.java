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
package org.openjdk.jigsaw.test.sat;

import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleSystem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openjdk.jigsaw.JigsawModuleSystem;
import org.openjdk.jigsaw.sat.ResolverException;
import org.openjdk.jigsaw.sat.Sat4JResolver;
import org.testng.Assert;

public abstract class AbstractResolverTest {

    protected ModuleSystem ms = JigsawModuleSystem.instance();

    protected MockLibrary mlib = new MockLibrary();

    protected Sat4JResolver r = new Sat4JResolver(mlib);

    protected void resolve(ModuleIdQuery[] midqs, Set<ModuleId> expectedMids) {
        Set<ModuleId> mids = r.resolve(midqs);
        System.out.println(mids);
        Assert.assertEquals(mids, expectedMids);        
        Assert.assertEquals(new ArrayList<>(mids), 
                new ArrayList<>(expectedMids));
    }
    
    protected void fail(ModuleIdQuery[] midqs) {
        ResolverException caught = null;
        Set<ModuleId> mids = null;
        try {
            mids = r.resolve(midqs);
        } catch (ResolverException ex) {
            caught = ex;
        }
        System.out.println(caught.getMessage());
        Assert.assertNotNull(caught, 
                "Resolver should fail but passed with the solution " + mids);
    }

    protected MockLibrary add(ModuleInfoBuilder mib) {
        return mlib.add(mib.build());
    }

    MockLibrary add(ModuleInfoBuilder.ModuleViewBuilder mvb) {
        return add(mvb.mib);
    }

    protected ModuleIdQuery[] queryIds(String... midqNames) {
        Set<ModuleIdQuery> midqs = new LinkedHashSet<>();
        for (String name : midqNames) {
            midqs.add(ms.parseModuleIdQuery(name));
        }
        return midqs.toArray(new ModuleIdQuery[0]);
    }

    protected Set<ModuleId> moduleIds(String... midNames) {
        Set<ModuleId> mids = new LinkedHashSet<>();
        for (String name : midNames) {
            mids.add(ms.parseModuleId(name));
        }
        return mids;
    }

    protected static ModuleInfoBuilder module(String id) {
        return ModuleInfoBuilder.module(id);
    }
}
