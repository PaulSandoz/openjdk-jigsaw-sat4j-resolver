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

public class ServiceResolverTest extends AbstractResolverTest {

    @Test
    public void testNoService() {
        add(module("x@1").
                requiresService("si"));

        resolve(queryIds("x@1"), moduleIds("x@1"));
    }
    
    @Test
    public void testOneServiceProvider() {
        add(module("x@1").
                requiresService("si"));

        add(module("b@1").
                providesService("si", "siImpl"));
        
        resolve(queryIds("x@1"), moduleIds("x@1", "b@1"));
    }
    
    @Test
    public void testMulipleServiceProviders() {
        add(module("x@1").
                requiresService("si"));

        add(module("b@1").
                providesService("si", "siImpl"));
        
        add(module("c@1").
                providesService("si", "siImpl"));
        
        add(module("d@1").
                providesService("si", "siImpl"));
        
        resolve(queryIds("x@1"), moduleIds("x@1", "b@1", "c@1", "d@1"));
    }
    
    @Test
    public void testVersions() {
        add(module("x@1").
                requiresService("si"));

        add(module("b@1").
                providesService("si", "siImpl"));
        
        add(module("b@2").
                providesService("si", "siImpl"));
        
        add(module("b@3").
                providesService("si", "siImpl"));
        
        resolve(queryIds("x@1"), moduleIds("x@1", "b@3"));
    }
    
    @Test
    public void testDependencies() {
        add(module("x@1").
                requires("c@1").
                requiresService("si"));

        add(module("b@1").
                requires("c@1").
                requires("d@1").
                providesService("si", "siImpl"));

        add(module("c@1"));
        
        add(module("d@1"));
        
        resolve(queryIds("x@1"), moduleIds("x@1", "c@1", "b@1", "d@1"));
    }
}
