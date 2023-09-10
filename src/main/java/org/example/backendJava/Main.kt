package org.example.backend_java

import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.math.pow

//september 2023
fun main() {
    third()
}

fun fourth() {
    var isNegative = 1
    val (n, c, d) = readln().split(" ").map {
        it.toInt()
    }
    val mistakes = readln().split(" ").map {
        if (it.toInt() < 0) isNegative *= -1
        it.toInt()
    } as MutableList
    if (isNegative == -1) {
        f(mistakes, true, c, d)
    } else {
        f(mistakes, false, c, d)

    }
    println(mistakes.joinTo(StringBuilder(""), " "))

}


fun f(array: MutableList<Int>, hasNegative: Boolean, c: Int, d: Int) {
    var hasNegative = hasNegative
    val max = 10.0.pow(9) + 1
    for (i in 0 until c) {
        var min = max.toInt()
        var minIndex = -1
        array.forEachIndexed { index: Int, i: Int ->
            if (abs(i) < min) {
                min = i
                minIndex = index
            }
        }
        if (hasNegative) {
            if (min < 0) {
                array[minIndex] -= d
            } else {
                array[minIndex] += d
            }
        } else {
            if (min < 0){
                array[minIndex] += d
                if (array[minIndex] >= 0){
                    hasNegative = true
                }
            } else {
                if (min - d < 0) hasNegative = true
                array[minIndex] -= d
            }

        }
    }
}


fun third() {
    val n = readln().toInt()
    val arrayN = readln().split(" ").map { it.toInt() }
    val m = readln().toInt()
    for (i in 0 until m) {
        val (s, f) = readln().split(" ").map { it.toInt() }
        if (f == s) println("Yes")
        else if (f - s == 1) {
            if (arrayN[f - 1] > arrayN[s - 1]) println("No")
            else println("Yes")
        } else {
            var state = true
            var flag = true
            for (j in s..f - 1) {
                if (state && arrayN[j] >= arrayN[j - 1]) continue
                if (state && arrayN[j] < arrayN[j - 1]) {
                    state = false
                    continue
                }
                if (!state && arrayN[j] <= arrayN[j - 1]) continue
                flag = false
                break
            }
            if (flag) println("Yes")
            else println("No")
        }
    }
}


fun second() {
    var (n, k) = readln().split(" ").map { it.toInt() }
    val list = readln().toCharArray()
    val map = mutableMapOf<Char, Int>()
    if (k >= n) println(0)
    else {
        for (i in list.indices) {
            val item = list[i]
            val mapItem = map[item]
            if (mapItem != null) {
                map[item] = mapItem + 1
            } else map[item] = 1
        }
        val list = map.entries.sortedBy { it.value }
        var uniq = list.size
        for (i in list.indices) {
            val item = list[i]
            if (k - item.value >= 0) {
                k -= item.value
                uniq--
            } else break
        }
        println(uniq)

    }
}


fun first() {
    var s = readln().toInt()
    var n = readln().toInt()
    val sum = ((s + 1) * s) / 2
    while (true) {
        val dif = n - sum
        if (dif >= 0) {
            n = dif
        } else break
    }
    while (true) {
        if (n - s >= 0) {
            n -= s--
        } else break

    }
    println(n)
}