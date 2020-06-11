package soot.jimple.infoflow.problems.lcrules;

import soot.SootMethod;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowManager;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.pattern.patterndata.PatternDataConstant;
import soot.jimple.infoflow.pattern.patterntag.LCExitFinishTag;
import soot.jimple.infoflow.problems.TaintPropagationResults;
import soot.jimple.infoflow.problems.rules.AbstractTaintPropagationRule;
import soot.jimple.infoflow.util.ByReferenceBoolean;

import java.util.Collection;
import java.util.Collections;

public class PatternAdaptationRule extends AbstractTaintPropagationRule{
    public PatternAdaptationRule(InfoflowManager manager, Abstraction zeroValue, TaintPropagationResults results) {
        super(manager, zeroValue, results);
    }
    @Override
    public Collection<Abstraction> propagateNormalFlow(Abstraction d1, Abstraction source, Stmt stmt, Stmt destStmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallFlow(Abstraction d1, Abstraction source, Stmt stmt, SootMethod dest, ByReferenceBoolean killAll) {
        return null;
    }

    @Override
    public Collection<Abstraction> propagateCallToReturnFlow(Abstraction d1, Abstraction source, Stmt stmt, ByReferenceBoolean killSource, ByReferenceBoolean killAll) {
        InvokeExpr exp = stmt.getInvokeExpr();
        //废弃，全部搬到InfoflowProblem里边去了
        //针对Pattern1的修正：当执行到finish()方法时，当前finish操作使得Abs的isfinishing变为true，被标有
//        if (exp.getMethod().getSignature().equals(PatternDataConstant.FINISHMETHODSIG)) {
//            killSource.value = true;
//            Abstraction newSource = source.deriveNewFinishingAbstraction(stmt);
//            return Collections.singleton(newSource);
//        }
        return null;
    }

    @Override
    public Collection<Abstraction> propagateReturnFlow(Collection<Abstraction> callerD1s, Abstraction source, Stmt stmt, Stmt retSite, Stmt callSite, ByReferenceBoolean killAll) {
        //不行，这部分要搬到problem里去
        //针对Pattern1的修正：当执行到主函数的return时（我们用LCExitFinishTag来进行标注），要把所有Abs的isfinishing设为false
        if (stmt.hasTag(LCExitFinishTag.TAG_NAME)) {

        }
        return null;
    }
}
