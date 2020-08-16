package soot.jimple.infoflow.pattern.patternresource.definition;

import soot.SootMethod;

public class LCMethodDefinition {
    protected SootMethod method = null;
    protected String methodsig = null;

    public SootMethod getMethod() {
        return method;
    }

    public String getMethodsig() {
        return methodsig;
    }

    public int[] getCheckParams() {
        return checkParams;
    }

    public boolean needCheckBase() {
        return checkBase;
    }

    public boolean needNotCheck() {return notcheck;}

    public boolean needCheckAssignedValue() {
        return checkAssignedValue;
    }

    protected int[] checkParams = null;
    protected boolean checkBase = true;
    protected boolean checkAssignedValue = false;
    protected boolean notcheck = false;
    public LCMethodDefinition(String methodsig, SootMethod method, boolean checkBase, boolean checkAssignedValue, int[] checkParams, boolean notcheck) {
        this.methodsig = methodsig;
        this.method = method;
        this.checkBase = checkBase;
        this.checkAssignedValue = checkAssignedValue;
        this.checkParams = checkParams;
        this.notcheck = notcheck;
    }
}
