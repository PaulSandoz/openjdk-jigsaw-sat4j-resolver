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

import org.testng.annotations.Test;

public class ViewAliasResolverTest extends AbstractResolverTest {

    @Test
    public void testSimpleView() {
        add(module("x@1").
                requires("yv@1"));

        add(module("y@1").view("yv"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@1"));
    }

    @Test
    public void testSimpleView2() {
        add(module("x@1").
                requires("yv@1"));

        add(module("y@1").view("yv"));

        add(module("z@1").requires("y@1"));

        resolve(queryIds("x@1", "z@1"), moduleIds("x@1", "y@1", "z@1"));
    }

    @Test
    public void testSimpleView3() {
        add(module("x@1").
                requires("yv@1"));

        add(module("y@1").view("yv"));

        add(module("z@1").requires("y@1"));

        resolve(queryIds("z@1", "x@1"), moduleIds("z@1", "y@1", "x@1"));
    }

    @Test
    public void testMultipleViews() {
        add(module("x@1").
                requires("yv1@1"));

        add(module("z@1").
                requires("yv2@1"));

        add(module("y@1").view("yv1").view("yv2"));

        resolve(queryIds("x@1", "z@1"), moduleIds("x@1", "y@1", "z@1"));
    }

    @Test
    public void testMultipleViewsToSameModule() {
        add(module("x@1").
                requires("yv1@1").
                requires("yv2@1"));

        add(module("y@1").view("yv1").view("yv2"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@1"));
    }

    @Test
    public void testViewWithRange() {
        add(module("x@1").
                requires("yv"));

        add(module("y@1").view("yv"));

        add(module("y@2").view("yv"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@2"));
    }

    @Test
    public void testAliasForSelf() {
        add(module("x@1").
                requires("xa@1").view("xv").alias("xa@1"));

        resolve(queryIds("x@1"), moduleIds("x@1"));
    }

    @Test
    public void testAliasForSelf2() {
        add(module("x@1").
                requires("xa@1").alias("xa@1"));

        resolve(queryIds("xa@1"), moduleIds("x@1"));
    }

    @Test
    public void testAliasForRoot() {
        add(module("x@1").
                alias("xa@1"));

        resolve(queryIds("xa@1"), moduleIds("x@1"));
    }

    @Test
    public void testSimpleAlias() {
        add(module("x@1").
                requires("ya@1"));

        add(module("y@1").alias("ya@1"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@1"));
    }

    @Test
    public void testSimpleAlias2() {
        add(module("x@1").
                requires("ya@1"));

        add(module("y@1").alias("ya@1"));

        add(module("z@1").requires("y@1"));

        resolve(queryIds("x@1", "z@1"), moduleIds("x@1", "y@1", "z@1"));
    }

    @Test
    public void testSimpleAlias3() {
        add(module("x@1").
                requires("ya@1"));

        add(module("y@1").alias("ya@1"));

        add(module("z@1").requires("y@1"));

        resolve(queryIds("z@1", "x@1"), moduleIds("z@1", "y@1", "x@1"));
    }

    @Test
    public void testAliasOfViewForRoot() {
        add(module("x@1").view("xv").alias("xva@1"));

        resolve(queryIds("xva@1"), moduleIds("x@1"));
    }

    @Test
    public void testAliasOfView() {
        add(module("x@1").
                requires("yva@1"));

        add(module("y@1").view("yv").alias("yva@1"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@1"));
    }

    @Test
    public void testAliasWithVersions() {
        add(module("x@1").
                requires("a"));

        add(module("b@1").
                alias("a@1"));

        add(module("b@2").
                alias("a@2"));

        add(module("b@3").
                alias("a@3"));

        resolve(queryIds("x@1"), moduleIds("x@1", "b@3"));
    }

    @Test
    public void testAliasWithVersionsReverse() {
        add(module("x@1").
                requires("a"));

        add(module("b@1").
                alias("a@3"));

        add(module("b@2").
                alias("a@2"));

        add(module("b@3").
                alias("a@1"));

        resolve(queryIds("x@1"), moduleIds("x@1", "b@3"));
    }
    
    @Test
    public void testAliasWithVersionsConstrained() {
        add(module("x@1").
                requires("a"));

        add(module("y@1").
                requires("a@3"));

        add(module("b@1").
                alias("a@3"));

        add(module("b@2").
                alias("a@2"));

        add(module("b@3").
                alias("a@1"));

        resolve(queryIds("x@1", "y@1"), moduleIds("x@1", "b@1", "y@1"));
    }
    
    @Test
    public void testAliasWithVersionsAndDifferentModules() {
        add(module("x@1").
                requires("a@1"));

        add(module("y@1").
                requires("a@3"));
        
        add(module("b@1").
                alias("a@1"));

        add(module("c@1").
                alias("a@2"));

        add(module("d@1").
                alias("a@3"));
        
        resolve(queryIds("x@1", "y@1"), moduleIds("x@1", "b@1", "y@1", "d@1"));
    }
    
    @Test
    public void testAliasWithVersionsAndDifferentModules2() {
        add(module("r@1").
                requires("x@1").
                requires("y@1"));
        
        add(module("x@1").
                requires("a@1"));

        add(module("y@1").
                requires("a@3"));
        
        add(module("b@1").
                alias("a@1"));

        add(module("c@1").
                alias("a@2"));

        add(module("d@1").
                alias("a@3"));
        
        resolve(queryIds("r@1"), moduleIds("r@1", "x@1", "b@1", "y@1", "d@1"));
    }
    
    @Test
    public void testOptional() {
        add(module("x@1").
                requiresOptional("a@1"));
        
        add(module("y@1").
                requiresOptional("a@3"));
        
        add(module("b@1").
                alias("a@1").
                requires("d@1"));
        
        add(module("c@1").
                alias("a@3"));
        
        resolve(queryIds("x@1", "y@1"), moduleIds("x@1", "y@1", "c@1"));
   }
    
    @Test
    public void testOptionalWithDifferentModules() {
        add(module("x@1").
                requiresOptional("a@<3"));

        add(module("y@1").
                requires("a@3"));

        add(module("b@1").
                alias("a@3"));

        add(module("d@1").
                alias("a@1").
                requires("e@1"));

        resolve(queryIds("x@1", "y@1"), moduleIds("x@1", "y@1", "b@1"));
    }

    @Test
    public void testOptionalWithVersions() {
        add(module("x@1").
                requiresOptional("a@<3"));

        add(module("y@1").
                requires("a@3"));

        add(module("b@1").
                alias("a@3"));

        add(module("b@2").
                alias("a@2"));

        add(module("b@3").
                alias("a@1").
                requires("d@1"));

        fail(queryIds("x@1", "y@1"));
    }
        
    @Test
    // ## Version of aliases are not taken into account
    // 
    public void testDivide() {
        add(module("x@1").
                requires("a"));
        
        add(module("agg@1").
                alias("a@1"));
        
        add(module("agg@2").
                alias("a@2"));
        
        add(module("a@3"));
        
//        resolve(queryIds("x@1"), moduleIds("x@1", "a@3"));
        resolve(queryIds("x@1"), moduleIds("x@1", "agg@2"));
    }
}