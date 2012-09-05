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
package org.openjdk.jigsaw.sat;

import java.io.IOException;
import java.lang.module.ModuleId;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleView;
import java.lang.module.ServiceDependence;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.openjdk.jigsaw.Catalog;

/**
 * Functionality to process service dependences of service consumer modules to
 * obtain corresponding service provider modules present in a catalog.
 */
public class ServiceDependences {

    private final Catalog c;

    // service interface to service provider midqs
    private final Map<String, Set<ModuleId>> interfaceToProviders;

    /**
     *
     * @param c catalog to look up service provider modules
     */
    public ServiceDependences(Catalog c) {
        this.c = c;
        this.interfaceToProviders = new HashMap<>();
    }

    /**
     * Get all service provider module ids for a collection of service consumer
     * module ids (or module view/aliase ids of).
     *
     * @param mids the service consumer module ids.
     * @return a modifiable set of service provider module ids of modules
     * present in the catalog.
     *
     * @throws IOException if there is an error using the catalog.
     */
    public Set<ModuleId> getProviderModules(Collection<ModuleId> mids) throws IOException {
        final Set<ModuleId> providers = new LinkedHashSet<>();

        for (ModuleId mid : mids) {
            getProviderModules(mid, providers);
        }

        return providers;
    }

    /**
     * Get all service provider module ids for a service consumer module id (or
     * module view/aliase ids of).
     *
     * @param mid the service consumer module id.
     * @return a modifiable set of service provider module ids of modules
     * present in the catalog.
     *
     * @throws IOException if there is an error using the catalog.
     */
    public Set<ModuleId> getProviderModules(ModuleId mid) throws IOException {
        return getProviderModules(mid, new LinkedHashSet<ModuleId>());
    }

    private Set<ModuleId> getProviderModules(ModuleId mid, Set<ModuleId> providers) throws IOException {
        final ModuleInfo mi = c.readModuleInfo(mid);

        final Set<ServiceDependence> serviceDeps = mi.requiresServices();
        if (!serviceDeps.isEmpty()) {
            for (ServiceDependence sd : mi.requiresServices()) {
                providers.addAll(getProviderModules(sd.service()));
            }
        }

        return providers;
    }

    /**
     * Get all service provider module ids for a service interface.
     *
     * @param serviceInterface the service interface.
     * @return an unmodifiable set of service provider module ids of modules
     * present in the catalog.
     *
     * @throws IOException if there is an error using the catalog.
     */
    public Set<ModuleId> getProviderModules(String serviceInterface) throws IOException {
        Set<ModuleId> providers = interfaceToProviders.get(serviceInterface);
        if (providers != null) {
            return providers;
        }

        providers = new LinkedHashSet<>();

        // ## This is very inefficient the library should contain
        // an index of service interface to set of service provider modules
        for (ModuleId providerId : c.listDeclaringModuleIds()) {
            final ModuleInfo provider = c.readModuleInfo(providerId);
            for (ModuleView providerView : provider.views()) {
                if (providerView.services().containsKey(serviceInterface)) {
                    providers.add(providerId);
                    break;
                }
            }
        }

        providers = Collections.unmodifiableSet(providers);
        interfaceToProviders.put(serviceInterface, providers);
        return providers;
    }
}
