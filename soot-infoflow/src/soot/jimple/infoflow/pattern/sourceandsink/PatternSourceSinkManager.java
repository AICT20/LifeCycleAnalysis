package soot.jimple.infoflow.pattern.sourceandsink;

import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.patterndata.PatternEntryData;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.manager.SinkInfo;
import soot.jimple.infoflow.sourcesSinks.manager.SourceInfo;
import soot.util.MultiMap;

import java.util.*;

//我们这里以全部的成员变量和所有的静态变量一次性算清
public class PatternSourceSinkManager implements IPatternSourceSinkManager {
    protected String appPackageName = null;
    protected Map<SootClass, PatternEntryData> entrypoints = null;
    protected Set<SootClass> allEntryClasses = null;
//    protected SootClass currentClass = null;
    protected Set<SootField> allActivityFields = null; //这里要包含所有Activity的成员变量
    protected Set<SootField> allStaticFields = null;//所有静态变量最后处理

    protected Map<SootField, SourceSinkDefinition> sources;
    protected Map<Stmt, SourceInfo> sourceInfos;

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
    public SourceInfo createSourceInfo(Stmt sCallSite, PatternInfoflowManager manager, MultiMap<SootClass, SootField> entrypoints) {
        //只处理assignment的情况，并且 r0 = r1.method()这类情况也要排除，因为在three-address-code里边是不会直接用field来操作的
        Set<SootClass> entryClasses = entrypoints.keySet();
        Set<SootField> entryFields = entrypoints.values();
        SourceSinkDefinition def = createSource(sCallSite, entryClasses, entryFields);
        return createSourceInfo(sCallSite, manager, def);
    }

    @Override  //额外检查一下，是否是entryClass本身或者其父类，子类的field
    public SourceInfo getSourceInfo(Stmt sCallSite, PatternInfoflowManager manager, SootClass entryClass) {
        //只处理assignment的情况，并且 r0 = r1.method()这类情况也要排除，因为在three-address-code里边是不会直接用field来操作的
        SourceSinkDefinition def = getSource(sCallSite, entryClass);
        return getSourceInfo(sCallSite, def);
    }

    //下面这个其实没有必要了
    @Override
    public SinkInfo getSinkInfo(Stmt sCallSite, PatternInfoflowManager manager, AccessPath ap) {
        return null;
    }


    @Override
    public void updateStaticFields(Stmt s) {
        SourceSinkDefinition def = getSource(s, null);
        if (null != def && def instanceof  PatternFieldSourceSinkDefinition && ((PatternFieldSourceSinkDefinition) def).isStatic()) {
            allStaticFields.add(((PatternFieldSourceSinkDefinition) def).getField());
        }
    }


    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    protected SourceSinkDefinition getSource(Stmt sCallSite, SootClass entryClass) {
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
        if (null != entryClass) {
            SootClass fieldClass = f.getDeclaringClass();
            if (fieldClass != entryClass && !Scene.v().getActiveHierarchy().isClassSubclassOf(fieldClass, entryClass)
                                       && !Scene.v().getActiveHierarchy().isClassSubclassOf(entryClass, fieldClass))
                return null;
        }

        {
            SourceSinkDefinition def = sources.get(f);
            if (def != null)
                return def;
        }
        return null;
    }

    protected SourceSinkDefinition createSource(Stmt sCallSite, Set<SootClass> entrypoints, Set<SootField> involvedFields) {
        SourceSinkDefinition def = getSource(sCallSite, null);
        if (null != def) {
            return def;
        }
        if (!(sCallSite instanceof  AssignStmt)) {
            return null;
        }

        AssignStmt ass = (AssignStmt)sCallSite;
        Value rop = ass.getRightOp();
        if (rop instanceof InstanceFieldRef || rop instanceof StaticFieldRef) {
            SootField f = ((FieldRef) rop).getField();
            if (!involvedFields.contains(f)) {
                return null;
            }
            if (!entrypoints.contains(f.getDeclaringClass())) {
                return null;
            }
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
            if (!entrypoints.contains(f.getDeclaringClass())) {
                return null;
            }
            SourceSinkDefinition newdef = new PatternFieldSourceSinkDefinition(((FieldRef) lop).getField(), lop instanceof  StaticFieldRef);
            sources.put(f, newdef);
            return newdef;
        }
        return null;
    }

    protected SourceInfo getSourceInfo(Stmt sCallSite, SourceSinkDefinition def) {
        if (def == null)
            return null;
        SourceInfo info = sourceInfos.get(sCallSite);
        return info;
    }

    protected SourceInfo createSourceInfo(Stmt sCallSite, PatternInfoflowManager manager, SourceSinkDefinition def) {
        if (!(sCallSite instanceof AssignStmt)) {
            return null;
        }
        SourceInfo info = getSourceInfo(sCallSite, def);
        if (null != info) {
            return info;
        }
        boolean shouldtaintsubfields = manager.getConfig().shouldTaintSubfieldsAndChildren();

        //注意下，a = r0.field 以及 r0.field = a 的情况，应该左右两边都要做
        if (def instanceof PatternFieldSourceSinkDefinition) {
            DefinitionStmt defStmt = (DefinitionStmt) sCallSite;
            Set<AccessPath> aps = new HashSet<>();
            Value leftop = defStmt.getLeftOp();
            aps.add(manager.getAccessPathFactory().createAccessPath(leftop, null,
                    null, null, shouldtaintsubfields, false, true, AccessPath.ArrayTaintType.ContentsAndLength, false));
            Value rightop = defStmt.getRightOp();
            AccessPath rightAp = null;
            if (rightop instanceof  Local || rightop instanceof FieldRef) {
                rightAp = manager.getAccessPathFactory().createAccessPath(rightop, null,
                        null, null, shouldtaintsubfields, false, true, AccessPath.ArrayTaintType.ContentsAndLength, false);
                aps.add(rightAp);
                if (rightAp instanceof Local) {

                }
            }
            PatternSourceInfo newsourceinfo = new PatternSourceInfo(def, aps);
            if (null != rightAp && rightop instanceof Local) {
                newsourceinfo.setApToSlice(rightAp);
            }
            sourceInfos.put(sCallSite, newsourceinfo);
            return newsourceinfo;
        }
        return null;

    }

}
