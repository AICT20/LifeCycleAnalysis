package soot.jimple.infoflow.pattern.result;

import soot.SootField;
import soot.SootMethod;
import soot.jimple.infoflow.pattern.patterndata.PatternData;
import soot.jimple.infoflow.pattern.patternresource.LCResourceOPList;

import java.util.HashMap;
import java.util.Map;

public class ErrorUsageDetailedReport {
    protected PatternData violatedPattern = null;
    protected SootField field = null;
    protected Map<String, LCResourceOPList> errorOps = null;
    public ErrorUsageDetailedReport() {
        errorOps = new HashMap<>();
    }
    public void addSatisfiedOPListForEntrypoint(String entrypointStr, LCResourceOPList list) {
        errorOps.put(entrypointStr, list);
    }
}
