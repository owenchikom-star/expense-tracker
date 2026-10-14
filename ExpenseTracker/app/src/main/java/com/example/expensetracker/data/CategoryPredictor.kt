package com.example.expensetracker.data
object CategoryPredictor {
    private val rules = mapOf(
        "Food" to listOf("takeaway", "restaurant", "uber eats", "checkers", "pick n pay", "shoprite", "woolworths", "woolies", "spar", "beef", "chicken", "coffee", "lunch", "dinner", "groceries","pizza","burger","fruits"),
        "Transport" to listOf("uber", "bolt", "gautrain", "taxi", "fuel", "petrol", "diesel", "parking", "etoll"),
        "Bills" to listOf("electricity", "eskom", "water", "municipal", "wifi", "fibre", "airtime", "data", "insurance", "rent", "bond", "school fees", "gas"),
        "Shopping" to listOf("takealot", "mr price", "zara", "h&m", "clothing", "shoes", "electronics"),
        "Health" to listOf("pharmacy", "clicks", "dis-chem", "doctor", "clinic", "hospital"),
        "Entertainment" to listOf("beers", "showmax", "DSTV", "champaign", "spotify", )
    )

    fun predict(name: String, amount: Double): String {
        val text = name.lowercase()

        // 1. Keyword match first
        rules.forEach { (category, keywords) ->
            if (keywords.any { text.contains(it) }) return category
        }

        // 2. Amount heuristics if no keyword
        return when {
            amount >= 5000 -> "Bills" // rent, insurance usually big
            amount in 200.0..1500.0 && text.contains("fuel") -> "Transport"
            amount < 50 -> "Food" // coffee, snacks
            else -> "Other"
        }
    }
}