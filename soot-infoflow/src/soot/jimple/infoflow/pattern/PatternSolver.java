package soot.jimple.infoflow.pattern;

import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.pattern.patterndata.Pattern1Data;
import soot.jimple.infoflow.pattern.patterndata.PatternData;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternSolver {
    private List<PatternData> patterns = null;

    public PatternSolver() {
        patterns = new ArrayList<PatternData>();
        patterns.add(new Pattern1Data());
    }

    public void searchForSeedMethods(BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        for (PatternData data : patterns) {
            data.searchForSeedMethods(icfg);
        }
    }

    public Set<Unit> getInitialSeeds() {
        Set<Unit> seeds = new HashSet<>();
        for (PatternData data : patterns) {
            seeds.addAll(data.getInitialSeeds());
        }
        return seeds;
    }



    public boolean shouldExitSensitivePosition(Unit u) {
        for (PatternData data : patterns) {
            if (data.isExitPoint(u)) {
                return true;
            }
        }
        return false;
    }

}
