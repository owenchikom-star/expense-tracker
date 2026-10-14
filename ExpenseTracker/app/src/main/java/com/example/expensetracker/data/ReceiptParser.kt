package com.example.expensetracker.data

import java.time.LocalDate
import java.util.Locale

/**
 * Advanced Receipt Parser designed to handle a wide range of international receipt formats.
 */
object ReceiptParser {

    private val AMOUNT_KEYWORDS = listOf(
        "total", "subtotal", "balance due", "amount due", "grand total", "net amount", "payable",
        "importe", "total a pagar", "montant", "somme", "netto", "brutto", "gesamt", "summe",
        "合計", "支付", "total geral", "totale", "totaal"
    )

    private val EXCLUDE_AMOUNT_KEYWORDS = listOf(
        "cash", "change", "tendered", "card", "vat", "tax", "iva", "mwst", "points", "loyalty",
        "saved", "discount", "off", "member", "savings", "balance", "gegeben", "geg"
    )

    private val EXCLUDE_STORE_KEYWORDS = listOf(
        "vat", "tel", "tax", "cashier", "receipt", "invoice", "change", "cash", "card", "tendered",
        "http", "www", "email", "address", "phone", "date", "time", "order", "terminal", "merchant",
        "customer", "copy", "reprint", "iva", "mwst", "auth", "approval", "transaction"
    )

    private val CURRENCY_SYMBOLS = listOf(
        "R", "$", "€", "£", "¥", "kr", "kn", "Rp", "zł", "złoty", "руб", "฿", "Rs", "RM", "EUR", "USD"
    )

    fun parse(text: String): ParsedReceipt {
        val lines = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        // 1. AMOUNT EXTRACTION
        val amount = extractAmount(lines)

        // 2. DATE EXTRACTION
        val date = extractDate(text) ?: LocalDate.now()

        // 3. STORE NAME EXTRACTION
        val storeName = extractStoreName(lines)

        // 4. CATEGORY PREDICTION
        val category = CategoryPredictor.predict(storeName + " " + text.take(300), amount)

        return ParsedReceipt(amount, date, category, storeName)
    }

    private fun extractAmount(lines: List<String>): Double {
        var bestCandidate: Double? = null

        // Strategy A: Scan for keywords (bottom-up)
        for (i in lines.indices.reversed()) {
            val rawLine = lines[i]
            val lineForKeyword = applyOCRFixesForKeywords(rawLine.lowercase())
            
            if (AMOUNT_KEYWORDS.any { lineForKeyword.contains(it) }) {
                val amountOnLine = extractAmountFromLine(rawLine)
                if (amountOnLine != null) {
                    bestCandidate = amountOnLine
                    break
                }
                if (i + 1 < lines.size) {
                    val amountNextLine = extractAmountFromLine(lines[i + 1])
                    if (amountNextLine != null) {
                        bestCandidate = amountNextLine
                        break
                    }
                }
            }
        }

        // Strategy B: Fallback to max amount
        if (bestCandidate == null || bestCandidate < 0.1) {
            val filteredLines = lines.filter { line ->
                val lower = applyOCRFixesForKeywords(line.lowercase())
                EXCLUDE_AMOUNT_KEYWORDS.none { lower.contains(it) }
            }
            
            val allAmounts = filteredLines.flatMap { line ->
                findAllAmountsInLine(line)
            }.filter { it > 1.0 && it < 100000.0 }
            
            bestCandidate = allAmounts.maxOrNull()
        }

        return bestCandidate ?: 0.0
    }

    private fun applyOCRFixesForKeywords(text: String): String {
        return text.replace("totai", "total")
            .replace("t0tal", "total")
            .replace("ge5amt", "gesamt")
            .replace("5umme", "summe")
    }

    private fun extractAmountFromLine(line: String): Double? {
        // Surgical OCR fixes for digits
        val fixedLine = line.replace(Regex("""(?<=\d)[oO]"""), "0")
            .replace(Regex("""[oO](?=\d)"""), "0")
            .replace(Regex("""(?<=\d)[iI]"""), "1")
            .replace(Regex("""[iI](?=\d)"""), "1")
            .replace(Regex("""(?<=\d)[sS]"""), "5")
            .replace(Regex("""[sS](?=\d)"""), "5")

        val symbolPattern = CURRENCY_SYMBOLS.joinToString("|") { Regex.escape(it) }
        val amountRegex = """(?i)(?:$symbolPattern)?\s*(\d+[\.,]\d{2})(?!\d)""".toRegex()
        val match = amountRegex.find(fixedLine)
        
        return match?.groupValues?.get(1)
            ?.replace(",", ".")
            ?.toDoubleOrNull()
    }

    private fun findAllAmountsInLine(line: String): List<Double> {
        val amountRegex = """(\d+[\.,]\d{2})(?!\d)""".toRegex()
        return amountRegex.findAll(line).mapNotNull { 
            it.groupValues[1].replace(",", ".")
                .toDoubleOrNull() 
        }.toList()
    }

    private fun extractDate(text: String): LocalDate? {
        val datePatterns = listOf(
            """\b(\d{4})[./-](\d{1,2})[./-](\d{1,2})\b""".toRegex(), // YYYY-MM-DD
            """\b(\d{1,2})[./-](\d{1,2})[./-](\d{2,4})\b""".toRegex(), // DD-MM-YY
            """\b(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{2,4})\b""".toRegex(RegexOption.IGNORE_CASE),
            """\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{2,4})\b""".toRegex(RegexOption.IGNORE_CASE)
        )

        for (pattern in datePatterns) {
            val match = pattern.find(text) ?: continue
            try {
                return when (pattern) {
                    datePatterns[0] -> {
                        val (y, m, d) = match.destructured
                        LocalDate.of(y.toInt(), m.toInt(), d.toInt())
                    }
                    datePatterns[1] -> {
                        val (d, m, y) = match.destructured
                        val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
                        LocalDate.of(year, m.toInt(), d.toInt())
                    }
                    datePatterns[2], datePatterns[3] -> {
                        val parts = match.groupValues
                        val (d, m, y) = if (pattern == datePatterns[2]) {
                            Triple(parts[1], parts[2], parts[3])
                        } else {
                            Triple(parts[2], parts[1], parts[3])
                        }
                        val month = parseMonthName(m)
                        val year = if (y.length == 2) 2000 + y.toInt() else y.toInt()
                        LocalDate.of(year, month, d.toInt())
                    }
                    else -> null
                }
            } catch (e: Exception) { }
        }
        return null
    }

    private fun parseMonthName(name: String): Int {
        return when (name.lowercase().take(3)) {
            "jan" -> 1 "feb" -> 2 "mar" -> 3 "apr" -> 4 "may" -> 5 "jun" -> 6
            "jul" -> 7 "aug" -> 8 "sep" -> 9 "oct" -> 10 "nov" -> 11 "dec" -> 12
            else -> 1
        }
    }

    private fun extractStoreName(lines: List<String>): String {
        val knownStores = listOf(
            "checkers", "pick n pay", "shoprite", "woolworths", "woolies", "super spar",
            "clicks", "dis-chem", "dischem", "shell", "bp", "caltex", "engen", 
            "mr price", "zara", "h&m", "takealot", "uber", "bolt", "steers", "wimpy", "kfc",
            "mcdonald", "starbucks", "subway", "tesco", "walmart", "target", "aldi", "lidl", "carrefour",
            "obc", "roots", "tops", "sana", "game", "chipiku plus"
        )

        // Strategy 1: Known stores first (prioritized)
        for (line in lines.take(5)) {
            val lower = line.lowercase()
            if (knownStores.any { lower.contains(it) }) return line
        }

        // Strategy 2: First line that isn't noise
        for (line in lines.take(5)) {
            val lower = line.lowercase()
            // Skip lines that contain digits (addresses, prices, dates)
            if (line.any { it.isDigit() }) continue
            // Skip keywords
            if (AMOUNT_KEYWORDS.any { lower.contains(it) }) continue
            if (EXCLUDE_STORE_KEYWORDS.any { lower.contains(it) }) continue

            if (line.length in 3..40) return line
        }

        return lines.getOrNull(0) ?: "Receipt"
    }
}
