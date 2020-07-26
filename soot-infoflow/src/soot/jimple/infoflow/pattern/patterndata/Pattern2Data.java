package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.*;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
//这里是Pattern2 :
// onSaveInstanceState 的生命周期发生了改变，变成了在onStop 之后，在API28之前的版本则在onStop之前，且不确定它与onPause的顺序

//注意，这种情况下onSaveInstanceState在前在后都需要分析
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


    @Override
    protected Map<SootClass, PatternEntryData> getInitialEntryClasses(Set<SootClass> allEntrypoints, IInfoflowCFG icfg) {
        Map<SootClass, PatternEntryData> newEntrypoints = new HashMap<>();
        for (SootClass cClass : allEntrypoints) {
            SootMethod onStopMethod = cClass.getMethodUnsafe(PatternDataConstant.ACTIVITY_ONSTOP);
            if (null == onStopMethod) {continue;}
            SootMethod onSaveMethod = cClass.getMethodUnsafe(PatternDataConstant.ACTIVITY_ONSAVEINSTANCESTATE);
            if (null == onSaveMethod) {continue;}
            newEntrypoints.put(cClass, new PatternEntryData(cClass));
        }
        return newEntrypoints;
    }

    protected void updateEntryDataWithLCMethods(SootClass cClass, PatternEntryData cData) {
        //这部分到时候再减少一些
        String[] methodsigs = new String[] {PatternDataConstant.ACTIVITY_ONSTOP, PatternDataConstant.ACTIVITY_ONSAVEINSTANCESTATE};
        for (String methodsig : methodsigs) {
            SootMethod lcmethod = cClass.getMethodUnsafe(methodsig);
            if (null != lcmethod) {
                Pair<String, SootMethod> pair = new Pair<>(methodsig, lcmethod);
                cData.add(pair);
            }
        }
    }


    public int getMinSdk() {
        return this.minSdk;
    }

    public int getTargetSdk() {
        return this.targetSdk;
    }

    public void clear() {
        super.clear();
        targetSdk = -1;
        minSdk = -1;
        shouldCheck = false;
    }

}
