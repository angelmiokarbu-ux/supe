package com.supe.assistant.data.parser

import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ExtractedBillInfo(
    val isBill: Boolean,
    val amount: Long?,
    val dueDate: Long?,
    val vendorOrTitle: String?
)

object BillExtractor {

    private val BILL_KEYWORDS = listOf(
        "tagihan", "jatuh tempo", "bayar", "bill", "due date", "pembayaran",
        "pln", "bpjs", "pdam", "indihome", "myrepublic", "telkomsel", "xl", "tri",
        "bca", "mandiri", "bri", "bni", "gopay", "ovo", "dana", "shopee", "shopeepay",
        "tokopedia", "spaylater", "kartu kredit", "invoice", "virtual account", "va "
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:rp|idr|sebesar|nominal|total)\s*[:.]?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+)""",
        Pattern.CASE_INSENSITIVE
    )

    private val SIMPLE_NUMBER_PATTERN = Pattern.compile(
        """([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{4,10})"""
    )

    fun parse(text: String, title: String = ""): ExtractedBillInfo {
        val combined = "$title $text".lowercase(Locale.ROOT)
        
        var isBill = BILL_KEYWORDS.any { combined.contains(it) }
        val amount = extractAmount(combined)
        if (amount != null && amount > 1000) {
            // Jika ada nominal uang dan judul mengandung kata transaksi/tagihan
            if (combined.contains("transfer") || combined.contains("bayar") || combined.contains("tagihan") || combined.contains("tempo")) {
                isBill = true
            }
        }

        val dueDate = extractDueDate(combined)
        val vendor = extractVendor(title.ifEmpty { text })

        return ExtractedBillInfo(
            isBill = isBill,
            amount = amount,
            dueDate = dueDate,
            vendorOrTitle = vendor
        )
    }

    fun extractAmount(text: String): Long? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(".", "")?.replace(",", "")?.trim()
            return raw?.toLongOrNull()
        }

        val simpleMatcher = SIMPLE_NUMBER_PATTERN.matcher(text)
        while (simpleMatcher.find()) {
            val raw = simpleMatcher.group(1)?.replace(".", "")?.replace(",", "")?.trim()
            val parsed = raw?.toLongOrNull()
            if (parsed != null && parsed in 5000..500000000) {
                return parsed
            }
        }
        return null
    }

    fun extractDueDate(text: String): Long? {
        val cal = Calendar.getInstance()

        if (text.contains("hari ini")) {
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            return cal.timeInMillis
        }
        if (text.contains("besok")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            return cal.timeInMillis
        }
        if (text.contains("lusa")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            return cal.timeInMillis
        }

        // Cari tanggal numerik: DD/MM atau DD/MM/YYYY
        val dateRegex = Regex("""\b([0-3]?[0-9])[\/\-]([0-1]?[0-9])(?:[\/\-](20[2-3][0-9]))?\b""")
        val match = dateRegex.find(text)
        if (match != null) {
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val year = match.groupValues.getOrNull(3)?.toIntOrNull() ?: cal.get(Calendar.YEAR)

            val parsedCal = Calendar.getInstance()
            parsedCal.set(Calendar.YEAR, year)
            parsedCal.set(Calendar.MONTH, month - 1)
            parsedCal.set(Calendar.DAY_OF_MONTH, day)
            parsedCal.set(Calendar.HOUR_OF_DAY, 18)
            parsedCal.set(Calendar.MINUTE, 0)
            return parsedCal.timeInMillis
        }

        return null
    }

    private fun extractVendor(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("pln") -> "Tagihan Listrik PLN"
            lower.contains("bpjs") -> "Iuran BPJS Kesehatan"
            lower.contains("pdam") -> "Tagihan Air PDAM"
            lower.contains("indihome") -> "Tagihan WiFi Indihome"
            lower.contains("myrepublic") -> "Tagihan WiFi MyRepublic"
            lower.contains("telkomsel") -> "Tagihan Pascabayar Telkomsel"
            lower.contains("spaylater") || lower.contains("shopee") -> "Tagihan Shopee / SPayLater"
            lower.contains("gopay") -> "Tagihan GoPay / GoPayLater"
            lower.contains("bca") -> "Bank BCA"
            lower.contains("mandiri") -> "Bank Mandiri"
            lower.contains("bri") -> "Bank BRI"
            lower.contains("dana") -> "DANA"
            else -> null
        }
    }

    fun formatRupiah(amount: Long?): String {
        if (amount == null) return "-"
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount).replace(",00", "")
    }
}
