package soot.jimple.infoflow.pattern.patterndata;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.Map;
import java.util.Set;

//pattern1 是在onCreate方法中调用finish()函数时将会跳过onStart、onResume、onPause和onStop方法的执行，直接跳到onDestroy
//TODO 在onStart方法中执行finish()时也会跳过onResume和onPause
public class Pattern1Data extends PatternData {
    public Pattern1Data(){super();};
    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
//        seedMethods = searchForSeedMethods(icfg, "void onDestroy()");
    }


    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalInvocationMap) {
        this.involvedEntrypoints.clear();
        for (SootMethod keym : totalInvocationMap.keySet()) {
            if (keym.getSubSignature().equals(PatternDataConstant.ONCREATESUBSIG) || keym.getSubSignature().equals(PatternDataConstant.ONSTARTSUBSIG)) {
                Set<SootMethod> invokedms = totalInvocationMap.get(keym);
                for (SootMethod invokedm : invokedms) {
                    if (invokedm.getSignature().equals(PatternDataConstant.FINISHMETHODSIG)) {
                        this.involvedEntrypoints.put(keym.getDeclaringClass(), keym.getSubSignature());
                    }
                }
            }
        }

    }

    @Override
    public void updateDummyMainMethod(SootMethod dummyMainMethod) {
        Body b = dummyMainMethod.getActiveBody();
        for (Unit u : b.getUnits()) {
            if (u instanceof Stmt && ((Stmt) u).containsInvokeExpr()) {
                InvokeExpr exp = ((Stmt) u).getInvokeExpr();
                SootMethod currentMainM = exp.getMethod();
                String content = involvedEntrypoints.get(currentMainM.getDeclaringClass());
                if (null == content) {
                    continue;
                }

                if (content.equals(PatternDataConstant.ONCREATESUBSIG)) {

                }
            }
        }
    }
}
