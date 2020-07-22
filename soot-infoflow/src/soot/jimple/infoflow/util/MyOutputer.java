package soot.jimple.infoflow.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyOutputer {
//    private int currentIndex = -1;
    private static MyOutputer instance = null;
    private File outputFile = null;
    private MyOutputer() {
        edgeNums = new HashMap<>();
        involvedEntries = new HashMap<>();
        minSdks = new HashMap<>();
        targetSdks = new HashMap<>();
    }
    private Map<Integer, Integer> edgeNums = null;
    private Map<Integer, Integer> involvedEntries = null;
    private Map<Integer, Integer> minSdks = null;
    private Map<Integer, Integer> targetSdks = null;
    public static MyOutputer getInstance() {
        if(null == instance) {
            instance = new MyOutputer();
        }
        return instance;
    }
//    public void updateIndex(int index) {
//        currentIndex = index;
//    }
    public void updateEdgeNums(int index, int num) {
        edgeNums.put(index, num);
    }
    public void updateInvolvedEntries(int index, int num) {
        involvedEntries.put(index, num);
    }
    public void updateMinSdk(int index, int minsdk) {
        minSdks.put(index, minsdk);
    }
    public void updateTargetSdk(int index, int targetsdk) {
        targetSdks.put(index, targetsdk);
    }
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
        if (!this.outputFile.exists()) {
            try {
                this.outputFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public synchronized void output() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(this.outputFile));
            for (Integer i : edgeNums.keySet()) {
                int edges = edgeNums.get(i);
                if ( involvedEntries.containsKey(i)) {
                    int entries = involvedEntries.get(i);
                    bw.write(i + "th app has " + edges + " edges and " + entries + " entries");
                    if (minSdks.containsKey(i) && targetSdks.containsKey(i)) {
                        bw.write("\t\t minSDK: " + minSdks.get(i) + "    targetSDK: " + targetSdks.get(i));
                    }
                } else {
                    bw.write(i + "th app has " + edges + " edges and no sources");
                }

                bw.newLine();
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
