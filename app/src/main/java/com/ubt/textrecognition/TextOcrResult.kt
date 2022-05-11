package com.ubt.textrecognition

class TextOcrResult {

    /**
     * model : chinese_ocr
     * code : 0
     * result : [{"pos":[[34,247],[552,250],[552,281],[34,279]],"text":"物于的14天内到达或途经：广东省深圳","prob":0.9158822298049927},{"pos":[[32,286],[65,291],[60,326],[27,321]],"text":"市","prob":0.9998924732208252}]
     */
    var model: String? = null
    var code = 0

    /**
     * pos : [[34,247],[552,250],[552,281],[34,279]]
     * text : 物于的14天内到达或途经：广东省深圳
     * prob : 0.9158822298049927
     */
    var result: List<Result>? = null

    class Result {
        var text: String? = null
        var prob = 0.0
        var pos: List<List<Int>>? = null
    }

    override fun toString(): String {
        val sb = StringBuilder()
        result?.forEach { r ->
            sb.append(r.text)
        }
        return sb.toString()
    }
}