package soot.jimple.infoflow.pattern.patterntag;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

//注意！！！！ 这里只标了Activity, Service，ContentProvider以及BroadCastReceiver，不包含Fragment
public class LCLifeCycleMethodTag implements Tag {
    public static String TAG_NAME = "lc_lc_method";
    private String currentName = null;

    public LCLifeCycleMethodTag(String newtagname) {
        currentName = TAG_NAME + "_" + newtagname;
    }

    @Override
    public String getName() {
        return currentName;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}
