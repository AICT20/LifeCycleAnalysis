package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PatternEntryData {
    protected SootClass entryclass = null;
    protected Set<Pair<String, SootMethod>> entrypoints = null;
    public PatternEntryData(SootClass entryclass) {
        this.entryclass = entryclass;
        this.entrypoints = new HashSet<>();
    }
    public void add(Pair<String, SootMethod> pair) {
        entrypoints.add(pair);
    }
    public void addAll(Set<Pair<String, SootMethod>> anotherEntrypoints) {
        if (null != anotherEntrypoints && !anotherEntrypoints.isEmpty()) {
            entrypoints.addAll(anotherEntrypoints);
        }
    }
    public Set<SootMethod> getAllMethods() {
        Set<SootMethod> allMethods = new HashSet<>();
        if (null != entrypoints) {
            for (Pair<String, SootMethod> pair : entrypoints) {
                allMethods.add(pair.getO2());
            }
        }
        return allMethods;
    }

    public boolean classEquals(PatternEntryData another) {
        if (null == another){
            return false;
        }
        if (another.entryclass != entryclass) {
            return false;
        }
        return true;
    }

    public boolean merge(PatternEntryData another) {
        if (null == another || !classEquals(another)) {
            return false;
        }
        this.entrypoints.addAll(another.entrypoints);
        return true;
    }
}
