/*
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.module.*;
import java.net.URI;
import java.security.CodeSigner;
import java.util.*;
import org.openjdk.jigsaw.*;

public class MockLibrary extends Library {

    private static JigsawModuleSystem jms = JigsawModuleSystem.instance();

    private Map<String, List<ModuleId>> idsForName = new HashMap<>();

    private Map<ModuleId, ModuleInfo> infoForId = new HashMap<>();

    public MockLibrary add(ModuleInfo mi) {
        for (ModuleView mv : mi.views()) {
            add(mi, mv.id());
            
            for (ModuleId aliasMid : mv.aliases()) {
                add(mi, aliasMid);
            }
        }
        return this;
    }

    private void add(ModuleInfo mi, ModuleId mid) {
            String name = mid.name();
            infoForId.put(mid, mi);
            List<ModuleId> ls = idsForName.get(name);
            if (ls == null) {
                ls = new ArrayList<>();
                idsForName.put(name, ls);
            }
            ls.add(mid);
    }
    
    public MockLibrary add(ModuleInfoBuilder mib) {
        return add(mib.build());
    }

    public MockLibrary add(ModuleInfoBuilder.ModuleViewBuilder mvb) {
        return add(mvb.mib);
    }

    private Map<ModuleId, List<String>> publicClassesForId = new HashMap<>();

    private Map<ModuleId, List<String>> otherClassesForId = new HashMap<>();

    MockLibrary add(ModuleId id, String cn,
            Map<ModuleId, List<String>> map) {
        List<String> ls = map.get(id);
        if (ls == null) {
            ls = new ArrayList<>();
            map.put(id, ls);
        }
        ls.add(cn);
        return this;
    }

    MockLibrary addPublic(String mids, String cn) {
        return add(jms.parseModuleId(mids), cn, publicClassesForId);
    }

    MockLibrary addOther(String mids, String cn) {
        return add(jms.parseModuleId(mids), cn, otherClassesForId);
    }

    public String name() {
        return "mock-library";
    }

    public int majorVersion() {
        return 0;
    }

    public int minorVersion() {
        return 1;
    }

    public URI location() {
        throw new UnsupportedOperationException();
    }

    public void installFromManifests(Collection<Manifest> mf) {
        throw new UnsupportedOperationException();
    }

    public void install(Collection<File> mf, boolean verifySignature) {
        throw new UnsupportedOperationException();
    }

    public Resolution resolve(Collection<ModuleIdQuery> midqs) {
        throw new UnsupportedOperationException();
    }

    public void install(Resolution res, boolean verifySignature) {
        throw new UnsupportedOperationException();
    }

    public Library parent() {
        return null;
    }

    protected void gatherLocalModuleIds(String mn, Set<ModuleId> mids) {
        if (idsForName.containsKey(mn)) {
            mids.addAll(idsForName.get(mn));
        }
    }

    protected void gatherLocalDeclaringModuleIds(Set<ModuleId> set) {
        set.addAll(infoForId.keySet());
    }

    public List<ModuleId> findModuleIds(String moduleName) {
        List<ModuleId> ls = idsForName.get(moduleName);
        if (ls == null) {
            ls = Collections.emptyList();
        }
        return ls;
    }

    public List<ModuleId> findModuleIds(ModuleIdQuery midq) {
        throw new UnsupportedOperationException();
    }

    public ModuleId findLatestModuleId(ModuleIdQuery midq) {
        throw new UnsupportedOperationException();
    }

    public ModuleInfo readLocalModuleInfo(ModuleId mid) {
        return infoForId.get(mid);
    }

    public byte[] readLocalModuleInfoBytes(ModuleId mid) {
        throw new UnsupportedOperationException();
    }

    public CodeSigner[] readLocalCodeSigners(ModuleId mid) {
        throw new UnsupportedOperationException();
    }

    public byte[] readLocalClass(ModuleId mid, String className) {
        throw new UnsupportedOperationException();
    }

    public void remove(List<ModuleId> mids, boolean dry) {
        throw new UnsupportedOperationException();
    }

    public void removeForcibly(List<ModuleId> mids) {
        throw new UnsupportedOperationException();
    }

    public ModuleId findModuleForClass(String className,
            ModuleId requestor)
            throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    public List<String> listLocalClasses(ModuleId mid, boolean all) {
        List<String> rv = new ArrayList<String>();
        List<String> pcns = publicClassesForId.get(mid);
        if (pcns != null) {
            rv.addAll(pcns);
        }
        if (all) {
            List<String> ocns = otherClassesForId.get(mid);
            if (ocns != null) {
                rv.addAll(ocns);
            }
        }
        return rv;
    }

    public Configuration<Context> readConfiguration(ModuleId mid) {
        throw new UnsupportedOperationException();
    }

    public URI findLocalResource(ModuleId mid, String name) {
        throw new UnsupportedOperationException();
    }

    public File findLocalNativeLibrary(ModuleId mid, String name) {
        throw new UnsupportedOperationException();
    }

    public File classPath(ModuleId mid) {
        throw new UnsupportedOperationException();
    }

    public RemoteRepositoryList repositoryList() throws IOException {
        return new RemoteRepositoryList() {
            public List<RemoteRepository> repositories() {
                return Collections.emptyList();
            }

            public RemoteRepository firstRepository() {
                return null;
            }

            public RemoteRepository add(URI uri, int position) {
                throw new UnsupportedOperationException();
            }

            public boolean remove(RemoteRepository rr) {
                throw new UnsupportedOperationException();
            }

            public boolean areCatalogsStale() {
                throw new UnsupportedOperationException();
            }

            public boolean updateCatalogs(boolean force) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
