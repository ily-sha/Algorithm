package org.example.internship

fun main() {
    val array = intArrayOf(8, 2, 4, 9, 3, 1)
}

fun third() {
    val n = readLine()!!.toInt()
    val my = readLine()!!.split(" ").map { it.toInt() }
    val win = readLine()!!.split(" ").map { it.toInt() }
    var l = 0
    var result = ""
    for (i in 0 until n) {
        if (win[i] == my[i]) {
            continue
        } else if (win[i] > my[i]) {
            result = "NO"
            break
        } else {
            l = i
            break
        }
    }
    if (result != "NO") {
        var r = n - 1
        for (i in n - 1 downTo 0) {
            if (win[i] == my[i]) {
                continue
            } else {
                r = i
                break
            }
        }
        val hmmy = HashMap<Int, Int>()
        val hmwin = HashMap<Int, Int>()
        for (i in l..r) {
            if (i != n && i != r && win[i] > win[i + 1]) {
                result = "NO"
                break
            }
            val myval = hmmy[my[i]]
            if (myval != null) {
                hmmy[my[i]] = myval + 1
            } else {
                hmmy[my[i]] = 1
            }
            val winmval = hmwin[win[i]]
            if (winmval != null) {
                hmwin[win[i]] = winmval + 1
            } else {
                hmwin[win[i]] = 1
            }
        }
        if (result != "NO") {
            if (hmwin == hmmy) {
                result = "YES"
            } else {
                result = "NO"
            }
        }
    }
    println(result)

}

fun fourth() {
    val (n, m) = readLine()!!.split(" ").map { it.toInt() }
    val list = ArrayList<Int>()
    val map = HashMap<Int, Int>()
    readLine()!!.split(" ").map {
        list.add(it.toInt())
        map[it.toInt()] = 0
    }
    val value = fourthHelper(n, map)
    if (value == null) println(-1)
    else {
        val list = mutableListOf<Int>()
        for (i in value) {
            for (j in 0 until i.value) {
                list.add(i.key)
            }
        }
        val result = StringBuilder("")
        println(list.size)
        println(list.joinTo(buffer = result, separator = " "))
    }
}


fun fourthHelper(sum: Int, map: Map<Int, Int>): Map<Int, Int>? {
    if (sum == 0) {
        return map
    } else if (sum < 0) {
        return null
    } else {
        for ((key, value) in map) {
            if (value != 2) {
                val newmap = map.toMutableMap()
                newmap[key] = value + 1
                val value = fourthHelper(sum - key, newmap)
                if (value != null) return value
            }
        }
        return null
    }
}

fun fifth() {
    val (n, m) = readLine()!!.split(" ").map { it.toInt() }
    val spiritsGroupCount = ArrayList<Int>(n)
    val map = HashMap<Int, Int>()
    val groupsNumbers = HashMap<Int, Set<Int>>()
    for (i in 0 until n) {
        map[i] = i
        groupsNumbers[i] = HashSet<Int>().apply {
            add(i)
        }
        spiritsGroupCount.add(1)
    }
    var nextGroupNumber = n
    for (i in 0 until m) {
        val request = readLine()!!.split(" ").map { it.toInt() }
        when (request[0]) {
            1 -> {
                val one = request[1] - 1
                val two = request[2] - 1
                val groupNumberOne = map[one]!!
                val groupNumberTwo = map[two]!!
                if (groupNumberOne != groupNumberTwo) {
                    val set1 = groupsNumbers[groupNumberOne]!!
                    val set2 = groupsNumbers[groupNumberTwo]!!
                    val set = HashSet<Int>()
                    for (j in set1) {
                        map[j] = nextGroupNumber
                        set.add(j)
                        spiritsGroupCount[j]++
                    }
                    for (j in set2) {
                        map[j] = nextGroupNumber
                        set.add(j)
                        spiritsGroupCount[j]++
                    }
                    groupsNumbers[nextGroupNumber++] = set

                }
            }

            2 -> {
                val one = request[1] - 1
                val two = request[2] - 1
                val groupNumberOne = map[one]!!
                val groupNumberTwo = map[two]!!
                if (groupNumberOne == groupNumberTwo) println("YES")
                else println("NO")
            }

            3 -> {
                println(spiritsGroupCount[request[1] - 1])
            }
        }
    }
}


fun second() {
    val map = HashMap<Char, Int>()
    map['s'] = 0
    map['h'] = 0
    map['r'] = 0
    map['e'] = 0
    map['f'] = 0
    map['i'] = 0
    val keys = map.keys
    readLine()?.map {
        if (keys.contains(it)) {
            map[it] = map[it]!! + 1
        }
    }
    val min = map.entries.minBy { it.value }
    if (min.value == 0 || (min.key == 'f' && min.value < 2)) {
        println(0)
    } else {
        if (min.key == 'f') {
            println(min.value / 2)
        } else {
            if (min.value * 2 <= map['f']!!) {
                println(min.value)
            } else {
                println(map['f']!! / 2)
            }
        }
    }
}


fun first() {
    var expensive = 0
    readLine()?.let {
        val (n, s) = it.split(" ").map { it.toInt() }
        readLine()?.let {
            it.split(" ").map {
                val current = it.toInt()
                if (current <= s && current > expensive) {
                    expensive = current
                }
            }
        }
    }
    println(expensive)
}
