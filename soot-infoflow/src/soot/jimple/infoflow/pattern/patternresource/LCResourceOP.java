package soot.jimple.infoflow.pattern.patternresource;

import soot.jimple.Stmt;
import soot.jimple.infoflow.data.AccessPath;

//描述某一成员变量上发生的操作
public class LCResourceOP {
    protected LCResourceOPType type = null;
    public LCResourceOP(AccessPath nowap, Stmt stmt) {
        //这部分通过stmt转化为type和其他重要值的部分放在
        this.type = null;
    }
}
