package soot.jimple.infoflow.pattern.patterndata;

import soot.SootMethod;
import soot.tagkit.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PatternDataConstant {
    public static final String ACTIVITY_ONCREATE = "void onCreate(android.os.Bundle)";
    public static final String ACTIVITY_ONSTART = "void onStart()";
    public static final String ACTIVITY_ONRESUME = "void onResume()";
    public static final String ACTIVITY_ONSAVEINSTANCESTATE = "void onSaveInstanceState(android.os.Bundle)";
    public static final String ACTIVITY_ONPAUSE = "void onPause()";
    public static final String ACTIVITY_ONSTOP = "void onStop()";
    public static final String ACTIVITY_ONRESTART = "void onRestart()";
    public static final String ACTIVITY_ONDESTROY = "void onDestroy()";

    public static final String[] ACTIVITY_LCMETHODS = {ACTIVITY_ONCREATE, ACTIVITY_ONSTART, ACTIVITY_ONRESUME,
            ACTIVITY_ONSAVEINSTANCESTATE, ACTIVITY_ONPAUSE, ACTIVITY_ONSTOP, ACTIVITY_ONRESTART, ACTIVITY_ONDESTROY};


    public static String FINISHMETHODSIG = "<android.app.Activity: void finish()>";

//    public static String LCMETHODSUFFIX = "lc_lc_method";
//    public static String LCMETHODNOTINSUFFIX = "lc_lc_method_none";
//
//
//    private static ConcurrentHashMap<SootMethod, Set<String>> tagcachemap = new ConcurrentHashMap<>();
//    public static void clear() {
//        tagcachemap.clear();
//    }
//    //下面
//    public static Set<String> getTagnames(SootMethod m) {
//        Set<String> currentTagnames = tagcachemap.get(m);
//        if (null == currentTagnames) {
//            currentTagnames = new HashSet<>();
//        }
//        for (Tag tag : m.getTags()) {
//            String tagname = tag.getName();
//            if (tagname.startsWith(LCMETHODSUFFIX)) {
//                currentTagnames.add(tagname);
//            }
//        }
//        if (currentTagnames.isEmpty()) {
//            tagcachemap.putIfAbsent(m, Collections.emptySet());
//        } else {
//            tagcachemap.putIfAbsent(m, currentTagnames);
//        }
//        return tagcachemap.get(m);
//    }
}
