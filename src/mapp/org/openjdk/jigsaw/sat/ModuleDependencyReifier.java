package org.openjdk.jigsaw.sat;

import java.lang.module.ModuleId;
import java.lang.module.ModuleIdQuery;
import java.lang.module.ModuleInfo;
import java.lang.module.ModuleView;
import java.lang.module.ViewDependence;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ModuleDependencyReifier implements ModuleGraphListener {

    // Map of module view id to module view
    // This is required because ModuleView does not specify equality
    final Map<ModuleId, ModuleView> idToView;

    // Root module view ids, grouped by module view name
    // In declaration order
    final Map<String, Set<ModuleId>> roots;

    // The set of default module view ids that the root module views depend on
    // directly or transitively
    // In topological order of depth first search
    final Set<ModuleId> modules;

    // View dependence to matching module view ids
    final Map<ViewDependence, Set<ModuleId>> dependenceToMatchingIds;

    public ModuleDependencyReifier() {
        idToView = new HashMap<>();
        roots = new LinkedHashMap<>();
        modules = new LinkedHashSet<>();
        dependenceToMatchingIds = new HashMap<>();
    }

    @Override
    public void onRootModuleDependency(ModuleIdQuery midq, ModuleView mv) {
        Set<ModuleId> mvs = roots.get(midq.name());
        if (mvs == null) {
            mvs = new LinkedHashSet();
            roots.put(midq.name(), mvs);
        }
        mvs.add(mv.id());
        idToView.put(mv.id(), mv);
    }

    @Override
    public void onModuleDependency(int depth, ModuleInfo rmi, ViewDependence vd, ModuleView mv) {
        modules.add(rmi.id());
        idToView.put(rmi.id(), rmi.defaultView());

        Set<ModuleId> mvs = dependenceToMatchingIds.get(vd);
        if (mvs == null) {
            mvs = new LinkedHashSet<>();
            dependenceToMatchingIds.put(vd, mvs);
        }
        mvs.add(mv.id());
        idToView.put(mv.id(), mv);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Set<ModuleId>> e : roots.entrySet()) {
            sb.append(e.getKey()).append(" -> ").append(e.getValue());
            sb.append("\n");
        }

        for (ModuleId mid : modules) {
            sb.append(mid).append("\n");

            ModuleView mv = idToView.get(mid);
            for (ViewDependence vd : mv.moduleInfo().requiresModules()) {
                sb.append("  ").append(vd).append(" -> ");

                Set<ModuleId> mids = dependenceToMatchingIds.get(vd);
                if (mids != null) {
                    sb.append(mids.toString());
                } else {
                    sb.append("[]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
