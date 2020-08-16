package soot.jimple.infoflow.pattern.sourceandsink;

import soot.PrimType;
import soot.SootField;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;
import soot.jimple.infoflow.util.TypeUtils;

//注意！！！！所有的sootfield相同的都做成同一个SourceSinkDefinition
public class PatternFieldSourceSinkDefinition extends SourceSinkDefinition {
    protected SootField field = null;
    protected boolean isImmutable = false;
//    protected boolean isLeft = true;//表示最初的taint为左边的元素
    protected boolean isStatic = false;
    public PatternFieldSourceSinkDefinition(SootField field, boolean isStatic) {
        this.field = field;
        this.isStatic = isStatic;
        if (!(field.getType() instanceof PrimType) && !(TypeUtils.isStringType(field.getType()))) {
            this.isImmutable = true;
        }

//        this.isLeft = isLeft;
//        this.isStatic = isStatic;
    }
//    public boolean isLeft() {
//        return this.isLeft;
//    }
    public boolean isStatic() {
        return this.isStatic;
    }
    public SootField getField() {
        return this.field;
    }

    @Override
    public SourceSinkDefinition getSourceOnlyDefinition() {
        return this;
    }

    @Override
    public SourceSinkDefinition getSinkOnlyDefinition() {
        return null;
    }

    @Override
    public void merge(SourceSinkDefinition other) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PatternFieldSourceSinkDefinition other = (PatternFieldSourceSinkDefinition) obj;
        if (other.field != field) {
            return false;
        }
        return true;
    }
}
