package soot.jimple.infoflow.pattern.patterndata;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Pattern3Data extends PatternData {
    private Map<SootClass, Set<SootClass>> v4Fragments = null;
    private Map<SootClass, Set<SootClass>> androidFragments = null;
    private Map<SootClass, Set<SootClass>> androidxFragments = null;
    public Pattern3Data(){
        super();
        this.v4Fragments = new HashMap<>();
        this.androidFragments = new HashMap<>();
        this.androidxFragments = new HashMap<>();
    }


    @Override
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
//        seedMethods = searchForSeedMethods(icfg, "void onSaveInstanceState(android.os.Bundle)");

    }


    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalInvocationMap) {

    }

    @Override
    public Set<SootClass> getEntrypoints() {
        Set<SootClass> allEntrypoints = new HashSet<>();
        if (null != v4Fragments) {
            allEntrypoints.addAll(v4Fragments.keySet());
        }
        if (null != androidFragments) {
            allEntrypoints.addAll(androidFragments.keySet());
        }
        if (null != androidxFragments) {
            allEntrypoints.addAll(androidxFragments.keySet());
        }
        return allEntrypoints;
    }

    public void updateFragments(SootClass activity, SootClass fragment, String typestr) {
        Map<SootClass, Set<SootClass>> insertMap = null;
        if (typestr.equalsIgnoreCase("v4")) {
            insertMap = v4Fragments;
        } else if (typestr.equalsIgnoreCase("android")) {
            insertMap = androidFragments;
        } else if (typestr.equalsIgnoreCase("androidx")) {
            insertMap = androidxFragments;
        }
        Set<SootClass> fragments = insertMap.get(activity);
        if (null == fragments){
            fragments = new HashSet<>();
            insertMap.put(activity, fragments);
        }
        fragments.add(fragment);
    }

    public Map<String, Map<SootClass, Set<SootClass>>> getAllFragments() {
        Map<String, Map<SootClass, Set<SootClass>>> results = new HashMap<>();
        if (!this.v4Fragments.isEmpty()) {
            results.put("V4", this.v4Fragments);
        }
        if (!this.androidFragments.isEmpty()) {
            results.put("android", this.androidFragments);
        }
        if (!this.androidxFragments.isEmpty()) {
            results.put("androidx", this.androidxFragments);
        }
        return results;
    }

    public void clear() {
        this.v4Fragments.clear();
        this.androidFragments.clear();
        this.androidxFragments.clear();
        this.involvedEntrypoints.clear();
    }
}
