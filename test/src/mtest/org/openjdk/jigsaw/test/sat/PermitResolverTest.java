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

import org.testng.annotations.Test;

public class PermitResolverTest extends AbstractResolverTest {

    @Test
    public void testNoPermitOnModule() {
        add(module("a@1").requires("b@1"));

        add(module("b@1").permits("c"));

        fail(queryIds("a@1"));
    }

    @Test
    public void testPermitOnModule() {
        add(module("a@1").requires("b@1"));

        add(module("b@1").permits("a"));

        resolve(queryIds("a@1"), moduleIds("a@1", "b@1"));
    }

    @Test
    public void testNoPermitOnView() {
        add(module("a@1").requires("bv@1"));

        add(module("b@1").view("bv").permits("c"));

        fail(queryIds("a@1"));
    }

    @Test
    public void testPermitOnView() {
        add(module("a@1").requires("bv@1"));

        add(module("b@1").view("bv").permits("a"));

        resolve(queryIds("a@1"), moduleIds("a@1", "b@1"));
    }

    @Test
    public void testPermitRange() {
        add(module("a@1").requires("b"));

        add(module("b@1").permits("c"));
        add(module("b@2").permits("a"));

        resolve(queryIds("a@1"), moduleIds("a@1", "b@2"));
    }

    @Test
    public void testPermitRange2() {
        add(module("a@1").requires("b"));

        add(module("b@1").permits("a"));
        add(module("b@2").permits("c"));

        resolve(queryIds("a@1"), moduleIds("a@1", "b@1"));
    }

    @Test
    public void testPermitWithOptional() {
        add(module("z@1")
                .requiresOptional("x"));
        
        add(module("x@1").permits("y"));

        resolve(queryIds("z@1"), moduleIds("z@1"));
    }

    @Test
    public void testPermitWithOptionalConflict() {
        add(module("z@1")
                .requires("y")
                .requiresOptional("x"));
        
        add(module("y@1").requires("x"));
        
        add(module("x@1").permits("y"));

        fail(queryIds("z@1"));
    }
}
