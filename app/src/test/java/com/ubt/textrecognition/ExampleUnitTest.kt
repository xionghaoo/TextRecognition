package com.ubt.textrecognition

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun t() {
        val txt = "您于前14天内到达或途经：云南省昆明\n" +
                "    市*（注：“表示当前该城市存在中风险\n" +
                "    或高风险地区，井不表示用户实际到访\n" +
                "    寸这些中高风险地区。）"
        if (txt.matches("^.*[\\*].*[中].*[高].*[风险].*$".toRegex())) {
            println("有带星号")
        } else {
            println("识别失败")
        }
    }
}