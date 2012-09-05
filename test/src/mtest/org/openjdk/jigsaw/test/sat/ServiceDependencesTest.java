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

import java.io.IOException;
import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleSystem;
import java.util.LinkedHashSet;
import java.util.Set;
import org.openjdk.jigsaw.JigsawModuleSystem;
import org.openjdk.jigsaw.sat.ServiceDependences;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ServiceDependencesTest {

    protected ModuleSystem ms = JigsawModuleSystem.instance();

    protected MockLibrary mlib;

    protected ServiceDependences sds;

    @BeforeMethod
    void before() {
        mlib = new MockLibrary();
        sds = new ServiceDependences(mlib);
    }

    @Test
    public void noModules() throws IOException {
        Assert.assertTrue(sds.getProviderModules(moduleIds("a@1")).isEmpty());
    }

    @Test
    public void noServiceConsumerModules() throws IOException {
        add(module("a@1"));
        add(module("b@1").requiresService("si"));

        Assert.assertTrue(sds.getProviderModules(moduleIds("a@1", "b@1")).isEmpty());
    }

    @Test
    public void serviceProviderModule() throws IOException {
        add(module("a@1").requiresService("si"));

        add(module("sip@1").
                providesService("si", "sip.siImpl1").
                providesService("si", "sip.siImpl2"));

        Assert.assertEquals(
                sds.getProviderModules(moduleIds("a@1")),
                moduleIds("sip@1"));
    }

    @Test
    public void serviceProviderModules() throws IOException {
        add(module("a@1").requiresService("si"));

        add(module("sip1@1").
                providesService("si", "sip1.siImpl1").
                providesService("si", "sip1.siImpl2"));
        add(module("sip1@2").
                providesService("si", "sip1.siImpl1").
                providesService("si", "sip1.siImpl2"));

        add(module("sip2@1").
                providesService("si", "sip2.siImpl1").
                providesService("si", "sip2.siImpl2"));
        add(module("sip2@2").
                providesService("si", "sip2.siImpl1").
                providesService("si", "sip2.siImpl2"));

        Assert.assertEquals(
                sds.getProviderModules(moduleIds("a@1")),
                moduleIds("sip1@1", "sip1@2", "sip2@1", "sip2@2"));
    }

    @Test
    public void serviceConsumerAndProviderModules() throws IOException {
        add(module("a@1").requiresService("si1"));
        add(module("b@1").requiresService("si2"));

        add(module("si1p1@1").
                providesService("si1", "si1p1.si1Impl1").
                providesService("si1", "si1p1.si1Impl2"));
        add(module("si1p1@2").
                providesService("si1", "si1p1.si1Impl1").
                providesService("si1", "si1p1.si1Impl2"));

        add(module("si1p2@1").
                providesService("si1", "si1p2.si1Impl1").
                providesService("si1", "si1p2.si1Impl2"));
        add(module("si1p2@2").
                providesService("si1", "si1p2.si1Impl1").
                providesService("si1", "si1p2.si1Impl2"));

        add(module("si2p1@1").
                providesService("si2", "si2p1.si2Impl1").
                providesService("si2", "si2p1.si2Impl2"));
        add(module("si2p1@2").
                providesService("si2", "si2p1.si2Impl1").
                providesService("si2", "si2p1.si2Impl2"));

        add(module("si2p2@1").
                providesService("si2", "si2p2.si2Impl1").
                providesService("si2", "si2p2.si2Impl2"));
        add(module("si2p2@2").
                providesService("si2", "si2p2.si2Impl1").
                providesService("si2", "si2p2.si2Impl2"));

        Assert.assertEquals(
                sds.getProviderModules(moduleIds("a@1", "b@1")),
                moduleIds("si1p1@1", "si1p1@2", "si1p2@1", "si1p2@2",
                "si2p1@1", "si2p1@2", "si2p2@1", "si2p2@2"));
    }

    @Test
    public void ingoreNotReferencedProviderModules() throws IOException {
        add(module("a@1").requiresService("si1"));
        add(module("b@1").requiresService("si2"));

        add(module("si1p1@1").
                providesService("si1", "si1p1.si1Impl1").
                providesService("si1", "si1p1.si1Impl2"));

        add(module("si2p1@1").
                providesService("si2", "si2p1.si2Impl1").
                providesService("si2", "si2p1.si2Impl2"));

        Assert.assertEquals(
                sds.getProviderModules(moduleIds("a@1")),
                moduleIds("si1p1@1"));
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
