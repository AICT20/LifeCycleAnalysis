package soot.jimple.infoflow.pattern.patterntag;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class LCExitFinishTag implements Tag {
    public static final String TAG_NAME = "lc_exit_finish";
    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
