package com.example.afica.lovetest.data.model

data class LoveTestMessage(
    val minPercentage: Int,
    val maxPercentage: Int,
    val message: String
)

data class LoveTestResult(val percentage: Int, val message: String)
