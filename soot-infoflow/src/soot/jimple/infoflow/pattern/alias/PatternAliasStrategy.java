package soot.jimple.infoflow.pattern.alias;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;
import soot.jimple.infoflow.pattern.solver.NormalSolver;
import soot.jimple.infoflow.pattern.solver.NormalState;
import soot.jimple.infoflow.pattern.solver.PatternInfoflowManager;

public class PatternAliasStrategy implements IPatternAliasingStrategy{
    protected PatternInfoflowManager manager = null;
    protected NormalSolver bSolver = null;
    public PatternAliasStrategy(PatternInfoflowManager manager, NormalSolver backwardsSolver) {
        this.manager = manager;
        this.bSolver = backwardsSolver;
    }

    //TODO 注意：1. ALias时应当只算当前edge中新发现的ap
    //           2. Alias时除了Alias外，当前ap在被发现前所执行的操作也要包含在内
    //           3. Alias计算时，当前的NormalState应当锁定，不能传播下去————比如放在一个general的map里存着，
    //             当Alias计算完成时，进行前向传播，并最终到达ActivationUnit时，再合并两个state
    @Override
    public void computeAliasTaints(final NormalState aliasState, final Stmt stmt) {
        for (Unit next : manager.getICFG().getSuccsOf(stmt)) {//注意：这里的icfg是BW的，因此getSucc才是从前面一句开始
            this.bSolver.propagate(next, aliasState, stmt);
        }
    }

    @Override
    public NormalSolver getSolver() {
        return this.bSolver;
    }

    @Override
    public void cleanup() {

    }
}
