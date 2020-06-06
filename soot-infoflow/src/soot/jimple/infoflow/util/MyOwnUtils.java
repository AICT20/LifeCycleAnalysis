package soot.jimple.infoflow.util;

import soot.Value;
import soot.jimple.infoflow.data.Abstraction;
import soot.jimple.infoflow.data.AccessPath;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MyOwnUtils {
    //需要确保set1和set2均不为空
    public static <T> boolean setCompare(Set<T> set1, Set<T> set2) {
        if (set1.size() != set2.size()) {
            return false;
        }
        Map<T, Integer> comparsionMap = new HashMap(set1.size());
        for (T t : set1) {
            comparsionMap.put(t, 1);
        }
        for (T t : set2) {
            if (comparsionMap.containsKey(t)) {
                comparsionMap.put(t, 2);
            } else {
                return false;
            }
        }
        for (Map.Entry<T, Integer> entry : comparsionMap.entrySet()) {
            if (entry.getValue() != 2) {
                return false;
            }
        }
        return true;
    }

    //用于找到Abstraction中，与给定value相同的ap
    public static Set<AccessPath> getEqualAP(Abstraction source, Value value) {
        Set<AccessPath> results = new HashSet();
        Set<AccessPath> aps = source.getAccessPaths();
        if (null == aps || aps.isEmpty()) {
            return results;
        }
        for (AccessPath ap : aps) {
            if (source == ap.getPlainValue()) {
                results.add(ap);
            }
        }
        return results;
    }
}
