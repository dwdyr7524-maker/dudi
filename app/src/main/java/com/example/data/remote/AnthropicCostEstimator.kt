package com.example.data.remote

import java.util.Locale

object AnthropicCostEstimator {

    data class ModelRates(
        val inputPerMillionUsd: Double,
        val outputPerMillionUsd: Double
    )

    fun ratesForModel(model: String): ModelRates {
        val lower = model.lowercase(Locale.US)
        return when {
            lower.contains("haiku") -> ModelRates(
                inputPerMillionUsd = 0.80,
                outputPerMillionUsd = 4.00
            )
            lower.contains("opus") -> ModelRates(
                inputPerMillionUsd = 15.00,
                outputPerMillionUsd = 75.00
            )
            else -> ModelRates(
                inputPerMillionUsd = 3.00,
                outputPerMillionUsd = 15.00
            )
        }
    }

    fun estimateCostUsd(
        model: String,
        inputTokens: Int,
        outputTokens: Int
    ): Double {
        if (inputTokens <= 0 && outputTokens <= 0) return 0.0
        val rates = ratesForModel(model)
        val inputCost = (inputTokens.coerceAtLeast(0) / 1_000_000.0) * rates.inputPerMillionUsd
        val outputCost = (outputTokens.coerceAtLeast(0) / 1_000_000.0) * rates.outputPerMillionUsd
        return inputCost + outputCost
    }

    fun formatUsd(amount: Double): String {
        return when {
            amount <= 0.0 -> "$0.00"
            amount < 0.01 -> String.format(Locale.US, "$%.4f", amount)
            else -> String.format(Locale.US, "$%.2f", amount)
        }
    }
}
