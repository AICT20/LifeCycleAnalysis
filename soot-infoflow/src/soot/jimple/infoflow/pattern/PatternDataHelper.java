package soot.jimple.infoflow.pattern;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.pattern.patterndata.*;

import java.util.*;

public class PatternDataHelper implements PatternInterface {
    String[] tags = null;
    Map<Integer, PatternData> currentPatterns = null;

    private Map<SootMethod, Set<SootMethod>> methodDirectInvokeMap = null;
    private Map<SootMethod, Set<SootMethod>> methodTotalInvokeMap = null;

    private static PatternDataHelper instance = new PatternDataHelper();
    private PatternDataHelper() {
        this.currentPatterns = new HashMap<>();
        this.methodDirectInvokeMap = new HashMap<>();
        this.methodTotalInvokeMap = new HashMap<>();
    }
    public static PatternDataHelper v() {
        return instance;
    }

    @Override
    public void updateInvolvedEntrypoints(Set<SootClass> allEntrypoints, Map<SootMethod, Set<SootMethod>> totalinvocationmap) {
        if (null == totalinvocationmap) {
            //先构建直接调用关系
            buildDirectInvokeMap(allEntrypoints);
            //再构建完全调用关系
            buildTotalInvokeMap();
        } else {
            methodTotalInvokeMap = totalinvocationmap;
        }

        for (PatternData pattern : currentPatterns.values()) {
            pattern.updateInvolvedEntrypoints(allEntrypoints, methodTotalInvokeMap);
        }


    }

    private void buildTotalInvokeMap() {
        for (SootMethod m : methodDirectInvokeMap.keySet()) {
            Stack<SootMethod> currentMethods = new Stack<>();
            Set<SootMethod> allMethods = new HashSet<>(methodDirectInvokeMap.get(m));
            currentMethods.addAll(methodDirectInvokeMap.get(m));
            while (!currentMethods.isEmpty()) {
                SootMethod currentMethod = currentMethods.pop();
                Set<SootMethod> currentInvokeMethods = methodDirectInvokeMap.get(currentMethod);
                if (null != currentInvokeMethods && !currentInvokeMethods.isEmpty()) {
                    for (SootMethod innerM : currentInvokeMethods) {
                        if (!allMethods.contains(innerM)) {
                            allMethods.add(innerM);
                            currentMethods.add(innerM);
                        }
                    }
                }
            }
            methodTotalInvokeMap.put(m, allMethods);
        }
    }

    private void buildDirectInvokeMap(Set<SootClass> allEntrypoints) {
        Stack<SootMethod> currentMethods = new Stack<>();
        Set<SootMethod> allMethods = new HashSet<>();
        for (SootClass c : allEntrypoints) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    currentMethods.add(m);
                    allMethods.add(m);
                }
            }
        }

        while (!currentMethods.isEmpty()){
            SootMethod m = currentMethods.pop();
            Set<SootMethod> invokedMethods = getAllInvokedMethodsInGivenMethod(m);
            Set<SootMethod> allInvokedMethods = methodDirectInvokeMap.get(m);
            if (null == allInvokedMethods) {
                allInvokedMethods = new HashSet<>();
                methodDirectInvokeMap.put(m, allInvokedMethods);
            }
            allInvokedMethods.addAll(invokedMethods);
            for (SootMethod innerM : invokedMethods) {
                if (!allMethods.contains(innerM)) {
                    allMethods.add(innerM);
                    currentMethods.push(innerM);
                }
            }
        }
    }

    private Set<SootMethod> getAllInvokedMethodsInGivenMethod(SootMethod givenM) {
        Set<SootMethod> returnMethods = new HashSet<>();
        Body b = null;
        try {
            b = givenM.getActiveBody();
        } catch (Exception e) {
            return returnMethods;
        }

        for (Unit u : b.getUnits()) {
            if (u instanceof Stmt && ((Stmt)u).containsInvokeExpr()) {
                InvokeExpr exp = ((Stmt) u).getInvokeExpr();
                returnMethods.add(exp.getMethod());
            }
        }
        return returnMethods;
    }


    @Override
    public void clear() {
        this.methodDirectInvokeMap.clear();
        this.methodTotalInvokeMap.clear();
        for (PatternData pattern : currentPatterns.values()) {
            pattern.clear();
        }
    }

    boolean hasPattern1 = false;
    boolean hasPattern2 = false;
    boolean hasPattern3 = false;


    public void init(String[] args, int targetSdk, int minSdk){
        if (null != args && args.length != 0) {
            tags = args;
            for (String arg : args) {
                if (arg.equals("1")) {
                    currentPatterns.put(1, new Pattern1Data());
                    hasPattern1 = true;
                } else if (arg.equals("2")){
                    currentPatterns.put(2, new Pattern2Data(targetSdk, minSdk));
                    hasPattern2 = true;
                } else if (arg.equals("3")) {
                    currentPatterns.put(3, new Pattern3Data());
                    hasPattern3 = true;
                }
            }
        }
    }

    public Pattern1Data getPattern1() {
        PatternData data = currentPatterns.get(1);
        return (null==data)?null:(Pattern1Data)data;
    }

    public boolean hasPattern1() {
        return hasPattern1;
    }

    public Pattern2Data getPattern2() {
        PatternData data = currentPatterns.get(2);
        return (null==data)?null:(Pattern2Data)data;
    }

    public boolean hasPattern2() {
        return hasPattern2;
    }
    public Pattern3Data getPattern3() {
        PatternData data = currentPatterns.get(3);
        return (null==data)?null:(Pattern3Data)data;
    }
    public boolean hasPattern3() {
        return hasPattern3;
    }
}
