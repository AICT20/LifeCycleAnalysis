package soot.jimple.infoflow.pattern.patterndata;

import soot.SootMethod;
import soot.tagkit.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PatternDataConstant {
    public static String FINISHMETHODSIG = "<android.app.Activity: void finish()>";
    public static String ONCREATESUBSIG = "void onCreate(android.os.Bundle)";
    public static String ONSTARTSUBSIG = "void onStart()";
    public static String LCMETHODSUFFIX = "lc_lc_method";
    public static String LCMETHODNOTINSUFFIX = "lc_lc_method_none";


    private static ConcurrentHashMap<SootMethod, Set<String>> tagcachemap = new ConcurrentHashMap<>();
    public static void clear() {
        tagcachemap.clear();
    }
    //下面
    public static Set<String> getTagnames(SootMethod m) {
        Set<String> currentTagnames = tagcachemap.get(m);
        if (null == currentTagnames) {
            currentTagnames = new HashSet<>();
        }
        for (Tag tag : m.getTags()) {
            String tagname = tag.getName();
            if (tagname.startsWith(LCMETHODSUFFIX)) {
                currentTagnames.add(tagname);
            }
        }
        if (currentTagnames.isEmpty()) {
            tagcachemap.putIfAbsent(m, Collections.emptySet());
        } else {
            tagcachemap.putIfAbsent(m, currentTagnames);
        }
        return tagcachemap.get(m);
    }
}
