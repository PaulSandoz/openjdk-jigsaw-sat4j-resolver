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

public class RequiresResolverTest extends AbstractResolverTest {

    @Test
    public void testSimple() {
        add(module("x@1").
                requires("y@1"));

        add(module("y@1"));

        resolve(queryIds("x@1"), moduleIds("x@1", "y@1"));
    }

    @Test
    public void testIntersectingRanges() {
        add(module("a@1").
                requires("b@<4").
                requires("c@1").
                requires("d@<4"));

        add(module("c@1").
                requires("b@>=2").
                requires("d@>=2"));

        add(module("b@1")).
                add(module("b@2")).
                add(module("b@3")).
                add(module("b@4"));

        add(module("d@1")).
                add(module("d@2")).
                add(module("d@3")).
                add(module("d@4"));

        resolve(queryIds("a@1"), 
                moduleIds("a@1", "b@3", "c@1", "d@3"));
    }

    @Test
    public void testDiamond() {
        add(module("x@1").requires("y@2").requires("w@4"));
        
        add(module("y@2").requires("z@>=3"));
        
        add(module("z@9"));
        
        add(module("z@4"));
        
        add(module("z@3"));
        
        add(module("w@4").requires("z@<=4"));
        
        resolve(queryIds("x@1"), 
                moduleIds("x@1", "y@2", "z@4", "w@4"));
    }
    
    @Test
    public void testDiamondFail() {
        add(module("x@1").requires("y@2").requires("w@4"));
        
        add(module("y@2").requires("z@<=3"));
        
        add(module("z@9"));
        
        add(module("z@4"));
        
        add(module("z@3"));
        
        add(module("w@4").requires("z@>=4"));
        
        fail(queryIds("x@1"));
    }
    
    @Test
    public void testMultiple() {
        add(module("x@1").requires("a@1").requires("b@1"));
        
        add(module("y@1").requires("c@1"));
        
        add(module("z@1").requires("b@1"));
        
        add(module("a@1").requires("b@1"));
        
        add(module("b@1").requires("c@1"));
        
        add(module("c@1"));
        
        resolve(queryIds("x@1", "y@1", "z@1"), 
                moduleIds("x@1", "a@1", "b@1", "c@1", "y@1", "z@1"));        
    }
    
    @Test
    public void testCycle() {
        add(module("x@1").requires("y@1"));
        
        add(module("y@1").requires("x@1"));
        
        resolve(queryIds("x@1"), 
                moduleIds("x@1", "y@1"));        
    }
}
