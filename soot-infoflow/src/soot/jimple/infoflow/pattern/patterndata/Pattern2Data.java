package soot.jimple.infoflow.pattern.patterndata;

import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
//这里是Pattern2 :
// onSaveInstanceState 的生命周期发生了改变，变成了在onStop 之后，在API28之前的版本则在onStop之前，且不确定它与onPause的顺序

//注意，这种情况下onSaveInstanceState在前在后都需要分析
//TODO onstop可能也需要分析？？？
public class Pattern2Data extends PatternData {
    private int targetSdk = -1;
    private int minSdk = -1;
    private boolean shouldCheck = false;
    public Pattern2Data(int targetSdk, int minSdk) {
        super();
        this.targetSdk = targetSdk;
        this.minSdk = minSdk;
        if (targetSdk >= 28 && minSdk < 28) {
            shouldCheck = true;
        }
    }

    public boolean shouldCheck() {
        return this.shouldCheck;
    }

    @Override
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
//        seedMethods = searchForSeedMethods(icfg, "void onSaveInstanceState(android.os.Bundle)");
    }


    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalInvocationMap) {
        if (shouldCheck) {
            this.involvedEntrypoints = new HashMap<>();
            for (SootClass nowclass: allEntrypoints) {
                involvedEntrypoints.put(nowclass, null);//Pattern2不涉及value值，只要key就行了
            }
        }

    }

    public int getMinSdk() {
        return this.getMinSdk();
    }

    public int getTargetSdk() {
        return this.getTargetSdk();
    }

}
