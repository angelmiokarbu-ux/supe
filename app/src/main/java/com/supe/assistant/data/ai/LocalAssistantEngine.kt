package com.supe.assistant.data.ai

import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.parser.BillExtractor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LocalAssistantEngine {

    private val dateFormatter = SimpleDateFormat("EEEE, d MMMM yyyy HH:mm", Locale("id", "ID"))
    private val shortDateFormatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))

    fun generateAnswer(userQuery: String, reminders: List<ReminderItem>): String {
        val query = userQuery.trim().lowercase(Locale("id", "ID"))
        val active = reminders.filter { !it.isCompleted }

        return when {
            // Cek sapaan umum
            query.contains("halo") || query.contains("hai") || query.contains("siapa kamu") -> {
                "Halo! Saya Supe, asisten pengingat dan pencatatan Anda. Anda dapat menanyakan total tagihan, jadwal jatuh tempo, atau mencari catatan yang tersimpan."
            }

            // Pertanyaan total tagihan
            query.contains("total tagihan") || query.contains("berapa tagihan") || query.contains("jumlah tagihan") -> {
                val bills = active.filter { it.category == "Tagihan" || it.amount != null }
                if (bills.isEmpty()) {
                    "Saat ini tidak ada catatan tagihan aktif yang belum dibayar."
                } else {
                    val total = bills.mapNotNull { it.amount }.sum()
                    val sb = StringBuilder()
                    sb.append("Total tagihan aktif Anda adalah **${BillExtractor.formatRupiah(total)}** (${bills.size} tagihan):\n\n")
                    bills.forEachIndexed { index, item ->
                        val amtStr = if (item.amount != null) " - ${BillExtractor.formatRupiah(item.amount)}" else ""
                        val dateStr = item.dueDate?.let { " (Jatuh tempo: ${shortDateFormatter.format(Date(it))})" } ?: ""
                        sb.append("${index + 1}. ${item.title}$amtStr$dateStr\n")
                    }
                    sb.toString().trim()
                }
            }

            // Jadwal Hari Ini
            query.contains("hari ini") -> {
                val todayCal = Calendar.getInstance()
                val todayStart = todayCal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                val todayEnd = todayCal.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.timeInMillis

                val todayItems = active.filter {
                    val due = it.dueDate
                    due != null && due in todayStart..todayEnd
                }

                if (todayItems.isEmpty()) {
                    "Tidak ada jadwal atau tagihan yang jatuh tempo hari ini. Semua terkelola dengan baik."
                } else {
                    val sb = StringBuilder("Berikut pengingat Anda untuk hari ini:\n\n")
                    todayItems.forEachIndexed { i, item ->
                        val timeStr = item.dueDate?.let { SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date(it)) } ?: ""
                        sb.append("${i + 1}. [Pukul $timeStr] ${item.title}\n")
                    }
                    sb.toString().trim()
                }
            }

            // Jadwal Besok
            query.contains("besok") -> {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
                val tomorrowEnd = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }.timeInMillis

                val tomorrowItems = active.filter {
                    val due = it.dueDate
                    due != null && due in tomorrowStart..tomorrowEnd
                }

                if (tomorrowItems.isEmpty()) {
                    "Tidak ada jadwal pengingat untuk besok."
                } else {
                    val sb = StringBuilder("Pengingat Anda untuk besok:\n\n")
                    tomorrowItems.forEachIndexed { i, item ->
                        sb.append("${i + 1}. ${item.title}\n")
                    }
                    sb.toString().trim()
                }
            }

            // Pencarian kata kunci spesifik (Listrik, Wifi, Bank, dll)
            query.any { char -> query.length > 2 } -> {
                val matches = reminders.filter {
                    it.title.lowercase(Locale("id", "ID")).contains(query) ||
                    it.content.lowercase(Locale("id", "ID")).contains(query) ||
                    (it.sourceApp?.lowercase(Locale("id", "ID"))?.contains(query) == true)
                }

                if (matches.isNotEmpty()) {
                    val sb = StringBuilder("Ditemukan ${matches.size} catatan terkait \"$userQuery\":\n\n")
                    matches.forEachIndexed { i, item ->
                        val status = if (item.isCompleted) " [SELESAI]" else " [AKTIF]"
                        val amtStr = if (item.amount != null) " (${BillExtractor.formatRupiah(item.amount)})" else ""
                        val dateStr = item.dueDate?.let { " - " + dateFormatter.format(Date(it)) } ?: ""
                        sb.append("${i + 1}. ${item.title}$amtStr$status$dateStr\n")
                        if (item.content.isNotBlank()) {
                            sb.append("   Catatan: ${item.content}\n")
                        }
                    }
                    sb.toString().trim()
                } else {
                    "Tidak ditemukan catatan atau pengingat yang cocok dengan \"$userQuery\". Saat ini ada ${active.size} pengingat aktif yang tersimpan di Supe."
                }
            }

            else -> {
                "Anda memiliki ${active.size} pengingat aktif. Tanyakan hal seperti: \"Total tagihan\", \"Jadwal hari ini\", atau kata kunci nama catatan Anda."
            }
        }
    }
}
