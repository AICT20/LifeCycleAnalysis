package soot.jimple.infoflow.util;

import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkDefinition;

import java.util.Set;

public class MyOwnUtils {
    //需要确保set1和set2均不为空
    public static <T> boolean setCompare(Set<T> set1, Set<T> set2) {
        if (set1.size() != set2.size()) {
            return false;
        }
        for (T t1 : set1) {
            if (!set2.contains(t1)) {
                return false;
            }
        }
        for (T t2 : set2) {
            if (!set1.contains(t2)) {
                return false;
            }
        }

//        Map<T, Integer> comparsionMap = new HashMap(set1.size());
//        for (T t : set1) {
//            comparsionMap.put(t, 1);
//        }
//        for (T t : set2) {
//            if (comparsionMap.containsKey(t)) {
//                comparsionMap.put(t, 2);
//            } else {
//                return false;
//            }
//        }
//        for (Map.Entry<T, Integer> entry : comparsionMap.entrySet()) {
//            if (entry.getValue() != 2) {
//                return false;
//            }
//        }
        return true;
    }


}
