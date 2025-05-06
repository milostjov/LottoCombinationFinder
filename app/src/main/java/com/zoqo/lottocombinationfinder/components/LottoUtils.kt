package com.zoqo.lottocombinationfinder.utils

import java.math.BigInteger

fun calculateTotalCombinations(total: Int, choose: Int): BigInteger {
    return factorial(total) / (factorial(choose) * factorial(total - choose))
}

fun factorial(n: Int): BigInteger {
    var result = BigInteger.ONE
    for (i in 1..n) {
        result *= BigInteger.valueOf(i.toLong())
    }
    return result
}

fun findCombination(rank: Int, total: Int, choose: Int): List<Int> {
    val combination = mutableListOf<Int>()
    var remainingRank = rank - 1 // Adjust for 0-based indexing
    var n = total
    var k = choose

    while (k > 0) {
        val binomial = calculateBinomial(n - 1, k - 1)

        if (remainingRank < binomial) {
            combination.add(total - n + 1)
            k--
        } else {
            remainingRank -= binomial
        }

        n--
    }

    return combination
}

fun calculateBinomial(n: Int, k: Int): Int {
    if (k == 0 || k == n) return 1
    return factorial(n).divide(factorial(k).multiply(factorial(n - k))).toInt()
}
