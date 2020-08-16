package soot.jimple.infoflow.pattern.patterndata;

import heros.solver.Pair;
import soot.*;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.IInfoflow;
import soot.jimple.infoflow.cfg.DefaultBiDiICFGFactory;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.util.Chain;

import java.util.*;

//pattern1 是在onCreate方法中调用finish()函数时将会跳过onStart、onResume、onPause和onStop方法的执行，直接跳到onDestroy
//这样的话，Pattern1应该包含的LCmethod有：onCreate(), onStart(), onStop(), onDestroy()
//争议点：onPause()以及onResume()是否也需要加入计算？

public class Pattern1Data extends PatternData {
    public Pattern1Data(){
        super();
    }
    protected Set<SootMethod> cannotSkipMethods = null;





    protected Map<SootClass, PatternEntryData> getInitialEntryClasses(Set<SootClass> allEntrypoints, IInfoflowCFG icfg) {
        SootMethod finishM = null;
        try {
            finishM = Scene.v().getMethod(PatternDataConstant.FINISHMETHODSIG);
        } catch (RuntimeException e) {
            finishM = null;
        }
//        //test
//        SootClass test = Scene.v().getSootClassUnsafe("com.klinker.android.twitter.activities.compose.Compose");
//        for (test)
//        //test
        if (null == finishM) {
            return Collections.EMPTY_MAP;
        }
        Set<SootMethod> callFinishMethod = new HashSet<>();
        Collection<Unit> finishInvocations = icfg.getCallersOf(finishM);
        for (Unit u : finishInvocations) {
            callFinishMethod.add(icfg.getMethodOf(u));
        }

        Stack<SootMethod> currentCallingMs = new Stack<>();
        currentCallingMs.addAll(callFinishMethod);
        while (!currentCallingMs.isEmpty()) {
            SootMethod cM = currentCallingMs.pop();
            Collection<Unit> invocations = icfg.getCallersOf(cM);
            for (Unit u : invocations) {
                SootMethod m  = icfg.getMethodOf(u);
                if (!callFinishMethod.contains(m)) {
                    callFinishMethod.add(m);
                    currentCallingMs.add(m);
                }
            }
        }
        //更新一下不可跳过的方法
        cannotSkipMethods = new HashSet<>();
        cannotSkipMethods.addAll(callFinishMethod);
        Map<SootClass, PatternEntryData> initalEntrypoints = new HashMap<>();
        for (SootMethod m : callFinishMethod) {
            String finishTag = m.getSubSignature();
            if (finishTag.equals(PatternDataConstant.ACTIVITY_ONCREATE) || finishTag.equals(PatternDataConstant.ACTIVITY_ONSTART)) {
                SootClass cClass = m.getDeclaringClass();
                PatternEntryData cData = initalEntrypoints.get(cClass);
                if (null == cData) {
                    cData = new PatternEntryData(cClass);
                }
                updateEntryDataWithLCMethods(cClass, cData);
                boolean hasInvolvedFields =  updateInvolvedFieldsInEntryDatas(icfg, cClass, cData);
                if (!hasInvolvedFields) {
                    initalEntrypoints.remove(cClass);
                } else {
                    initalEntrypoints.put(cClass, cData);
                }
            }
        }
        return  initalEntrypoints;
    }

    protected void updateEntryDataWithLCMethods(SootClass cClass, PatternEntryData cData) {
        String[] methodsigs = new String[] {PatternDataConstant.ACTIVITY_ONCREATE, PatternDataConstant.ACTIVITY_ONSTOP};
        for (String methodsig : methodsigs) {
            SootMethod lcmethod = cClass.getMethodUnsafe(methodsig);
            if (null != lcmethod) {
                Pair<String, SootMethod> pair = new Pair<>(methodsig, lcmethod);
                cData.add(pair);
            }
        }
    }

    public Set<SootMethod> getCannotSkipMethods() {
        return this.cannotSkipMethods;
    }
}
