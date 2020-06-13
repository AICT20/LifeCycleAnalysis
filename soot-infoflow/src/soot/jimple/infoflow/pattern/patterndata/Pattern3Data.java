package soot.jimple.infoflow.pattern.patterndata;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.Map;
import java.util.Set;

public class Pattern3Data extends PatternData {
    @Override
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
//        seedMethods = searchForSeedMethods(icfg, "void onSaveInstanceState(android.os.Bundle)");

    }


    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalInvocationMap) {

    }

}
