package soot.jimple.infoflow.android.source;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.patterndata.PatternEntryData;
import soot.jimple.infoflow.sourcesSinks.definitions.PatternFieldSourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.ISourceSinkManager;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;

import java.util.*;

//我们这里以全部的成员变量和所有的静态变量一次性算清
public class PatternSourceSinkManager implements ISourceSinkManager {
    protected String appPackageName = null;
    protected Map<SootClass, PatternEntryData> entrypoints = null;
    protected Set<SootClass> allEntryClasses = null;
//    protected SootClass currentClass = null;
    protected Set<SootField> allActivityFields = null; //这里要包含所有Activity的成员变量
    protected Set<SootField> allStaticFields = null;//所有静态变量最后处理

    protected Map<SootField, SourceSinkDefinition> sources;
    protected Map<SourceSinkDefinition, SourceInfo> sourceInfos;

    public PatternSourceSinkManager( Map<SootClass, PatternEntryData> entrypoints) {
        this.entrypoints = entrypoints;
    }

    @Override
    public void initialize() {
        allEntryClasses = new HashSet<>();
        allEntryClasses.addAll(this.entrypoints.keySet());
        allActivityFields = new HashSet<>();
        sources = new HashMap<>();
        sourceInfos = new HashMap<>();
        for (SootClass cClass : allEntryClasses) {
            allActivityFields.addAll(cClass.getFields());
        }
        allStaticFields = new HashSet<>();
    }

    @Override
    public SourceInfo createSourceInfo(Stmt sCallSite, InfoflowManager manager) {
        //只处理assignment的情况，并且 r0 = r1.method()这类情况也要排除，因为在three-address-code里边是不会直接用field来操作的
        SourceSinkDefinition def = createSource(sCallSite);
        return createSourceInfo(sCallSite, manager, def);
    }

    @Override
    public SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager) {
        //只处理assignment的情况，并且 r0 = r1.method()这类情况也要排除，因为在three-address-code里边是不会直接用field来操作的
        SourceSinkDefinition def = getSource(sCallSite);
        return getSourceInfo(sCallSite, manager, def);
    }

    //下面这个其实没有必要了
    @Override
    public SinkInfo getSinkInfo(Stmt sCallSite, InfoflowManager manager, AccessPath ap) {
        return null;
    }


    @Override
    public void updateStaticFields(Stmt s) {
        SourceSinkDefinition def = getSource(s);
        if (null != def && def instanceof  PatternFieldSourceSinkDefinition && ((PatternFieldSourceSinkDefinition) def).isStatic()) {
            allStaticFields.add(((PatternFieldSourceSinkDefinition) def).getField());
        }
    }


    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    protected SourceSinkDefinition getSource(Stmt sCallSite) {
        if (!(sCallSite instanceof AssignStmt) || sCallSite.containsInvokeExpr()) {
            return null;
        }
        SootField f = null;
        Value rop = ((AssignStmt) sCallSite).getRightOp();
        if (rop instanceof InstanceFieldRef || rop instanceof StaticFieldRef) {
            f = ((FieldRef) rop).getField();
        }
        Value lop = ((AssignStmt) sCallSite).getLeftOp();
        if (lop instanceof InstanceFieldRef || lop instanceof StaticFieldRef) {
            f = ((FieldRef) lop).getField();
        }
        {
            SourceSinkDefinition def = sources.get(f);
            if (def != null)
                return def;
        }
        return null;
    }

    protected SourceSinkDefinition createSource(Stmt sCallSite) {
        SourceSinkDefinition def = getSource(sCallSite);
        if (null != def) {
            return def;
        }

        AssignStmt ass = (AssignStmt)sCallSite;
        Value rop = ass.getRightOp();
        if (rop instanceof InstanceFieldRef || rop instanceof StaticFieldRef) {
            SootField f = ((FieldRef) rop).getField();
            SourceSinkDefinition newdef = new PatternFieldSourceSinkDefinition(f, rop instanceof  StaticFieldRef);
            sources.put(f, newdef);
            return newdef;
        }
        //还需要注意下
        //a.field = r0;
        //r0.save
        //这样的情况
        Value lop = ass.getLeftOp();
        if (lop instanceof InstanceFieldRef || lop instanceof StaticFieldRef) {
            SootField f = ((FieldRef) lop).getField();
                SourceSinkDefinition newdef = new PatternFieldSourceSinkDefinition(((FieldRef) lop).getField(), lop instanceof  StaticFieldRef);
                sources.put(f, newdef);
                return newdef;
//            }
        }
        return null;
    }

    protected SourceInfo getSourceInfo(Stmt sCallSite, InfoflowManager manager, SourceSinkDefinition def) {
        if (def == null)
            return null;
        SourceInfo info = sourceInfos.get(sCallSite);
        return info;
    }

    protected SourceInfo createSourceInfo(Stmt sCallSite, InfoflowManager manager, SourceSinkDefinition def) {
        SourceInfo info = getSourceInfo(sCallSite, manager, def);
        if (null != info) {
            return info;
        }

        // If we don't have an invocation, we just taint the left side of the
        // assignment

        if (def instanceof PatternFieldSourceSinkDefinition) {
            DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
            Value taintob = defStmt.getLeftOp();
            return new SourceInfo(def, manager.getAccessPathFactory().createAccessPath(taintob, null,
                    null, null, false, false, true, AccessPath.ArrayTaintType.ContentsAndLength, false));
        }
        return null;

    }

}
