package soot.jimple.infoflow.pattern.patterndata;

import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.*;

public abstract class PatternData implements PatternInterface{
    protected Set<SootMethod> seedMethods = null;//这里存储的都是onDestroy的方法
    protected BiDiInterproceduralCFG<Unit, SootMethod> icfg = null;
    protected Map<SootClass, String> involvedEntrypoints = null;
    public PatternData() {
        this.involvedEntrypoints = new HashMap<>();
    }

    public Set<Unit> getInitialSeeds() {
        Set<Unit> seeds = new HashSet<Unit>();
        for (SootMethod m : seedMethods) {
            for (Unit u: icfg.getStartPointsOf(m)) {
                seeds.add(u);
            }
        }

        return seeds;
    }
    public boolean isExitPoint(Unit u) {
        if (u instanceof Stmt) {
            if ( ((Stmt)u).containsInvokeExpr()) {
                SootMethod m = ((Stmt)u).getInvokeExpr().getMethod();
                //说明是对这些onDestroy method的调用
                if (seedMethods.contains(m)) {
                    return true;
                }
            }
        }
        return false;
    }



    abstract public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg); //这个只是npe用，resource leak的话它的seed比较少，比较好控制
    protected Set<SootMethod> searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg, String methodname){
        seedMethods = new HashSet<SootMethod>();
        this.icfg = icfg;
        if (Scene.v().hasCallGraph()) {
            ReachableMethods reachableMethods = Scene.v().getReachableMethods();
            reachableMethods.update();
            for (Iterator<MethodOrMethodContext> iter = reachableMethods.listener(); iter.hasNext();) {
                SootMethod sm = iter.next().method();
                if (sm.getSubSignature().equals(methodname) && !sm.getDeclaringClass().isLibraryClass()) {//这里可以更加详细些
                    seedMethods.add(sm);
                }
                //下面是测试用的
                if (sm.getSubSignature().contains("onSaveInstance")) {
                    System.out.println();
                }
            }
        } else {
            System.err.println("call graph loading for patterns error!!!");
        }
        return seedMethods;
    }

    public void clear() {
        this.involvedEntrypoints.clear();
    }

    public String getFinishLocation(SootClass givenclass) {
        return this.involvedEntrypoints.get(givenclass);
    }

}
