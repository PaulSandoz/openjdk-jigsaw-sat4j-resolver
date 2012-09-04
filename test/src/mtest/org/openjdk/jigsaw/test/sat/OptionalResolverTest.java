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

public class OptionalResolverTest extends AbstractResolverTest {

    @Test
    public void testOptional() {
        add(module("a@1").
                requiresOptional("b@>=1").
                requires("c@1"));

        add(module("c@1").
                requiresOptional("b@>=2").
                requires("d@2"));
                
        add(module("b@1").requires("d@1"));
        add(module("b@2").requires("d@1"));
        add(module("b@3").requires("d@1"));
        add(module("b@4").requires("d@1"));

        add(module("d@1"));
        add(module("d@2"));
                
        resolve(queryIds("a@1"), moduleIds("a@1", "c@1", "d@2"));
    }
    
    @Test
    public void testOptional2() {
        add(module("a@1").
                requiresOptional("b@>=1").
                requires("c@1"));

        add(module("c@1").
                requiresOptional("b@>=2").
                requires("d@2"));
                
        add(module("b@1"));
        add(module("b@2"));
        add(module("b@3").requires("d@1"));
        add(module("b@4").requires("d@1"));

        add(module("d@1"));
        add(module("d@2"));
                
        resolve(queryIds("a@1"), moduleIds("a@1", "b@2", "c@1", "d@2"));
    }
    
    @Test
    public void testOptional3() {
        add(module("a@1").
                requiresOptional("b@>=1").
                requires("c@1"));

        add(module("c@1").
                requiresOptional("b@>=2").
                requires("d@2"));
                
        add(module("b@1"));
        add(module("b@2"));
        add(module("b@3"));
        add(module("b@4"));

        add(module("d@1"));
        add(module("d@2"));
                
        resolve(queryIds("a@1"), moduleIds("a@1", "b@4", "c@1", "d@2"));
    }    
    
    
    @Test
    public void testOptionalFail() {
        add(module("a@1").
                requiresOptional("b@<2").
                requires("c@1"));

        add(module("c@1").
                requires("b@>=2").
                requires("d@2"));
                
        add(module("b@1").requires("d@1"));
        add(module("b@2"));

        add(module("d@1"));
        add(module("d@2"));
                
        fail(queryIds("a@1"));
    }
}
