package soot.jimple.infoflow.pattern.patterndata;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

//pattern1 是在oncreate 方法中调用isfinishing函数时将会跳过onStart、onResume、onPause和onStop方法的执行，直接跳到onDestroy
//TODO 在onStart方法中执行isfinishing时也会跳过onResume和onPause
public class Pattern1Data extends PatternData {
    public Pattern1Data(){};
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        seedMethods = searchForSeedMethods(icfg, "void onDestroy()");
    }



}
