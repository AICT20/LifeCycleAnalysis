package soot.jimple.infoflow.pattern.patterndata;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
//这里是Pattern2 :
// onSaveInstanceState 的生命周期发生了改变，变成了在onStop 之后，在API28之前的版本则在onStop之前，且不确定它与onPause的顺序

//注意，这种情况下onSaveInstanceState在前在后都需要分析
//TODO onstop可能也需要分析？？？
public class Pattern2Data extends PatternData {

    @Override
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        seedMethods = searchForSeedMethods(icfg, "void onSaveInstanceState(android.os.Bundle)");
    }

}
