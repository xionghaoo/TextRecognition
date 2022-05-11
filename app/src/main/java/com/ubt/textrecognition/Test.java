package com.ubt.textrecognition;

import java.util.List;

public class Test {

    /**
     * model : chinese_ocr
     * code : 0
     * result : [{"pos":[[34,247],[552,250],[552,281],[34,279]],"text":"物于的14天内到达或途经：广东省深圳","prob":0.9158822298049927},{"pos":[[32,286],[65,291],[60,326],[27,321]],"text":"市","prob":0.9998924732208252}]
     */

    public String model;
    public int code;
    /**
     * pos : [[34,247],[552,250],[552,281],[34,279]]
     * text : 物于的14天内到达或途经：广东省深圳
     * prob : 0.9158822298049927
     */

    public List<Result> result;

    public static class Result {
        public String text;
        public double prob;
        public List<List<Integer>> pos;
    }
}
