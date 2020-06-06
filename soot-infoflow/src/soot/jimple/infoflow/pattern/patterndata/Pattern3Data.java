package soot.jimple.infoflow.pattern.patterndata;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

public class Pattern3Data extends PatternData {
    @Override
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        seedMethods = searchForSeedMethods(icfg, "void onSaveInstanceState(android.os.Bundle)");

    }
}
