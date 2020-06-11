package soot.jimple.infoflow.pattern.patterntag;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class LCFinishBranchTag implements Tag {
    public static final String TAG_NAME = "lc_finish_branch";

    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
