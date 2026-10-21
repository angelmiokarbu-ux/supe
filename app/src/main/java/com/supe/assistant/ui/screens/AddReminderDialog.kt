package com.supe.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType
import com.supe.assistant.data.parser.BillExtractor
import java.util.Calendar
import java.util.UUID

@Composable
fun AddReminderDialog(
    initialTitle: String = "",
    initialContent: String = "",
    initialAmount: Long? = null,
    initialType: ReminderType = ReminderType.TEXT,
    initialImageUri: String? = null,
    initialAudioPath: String? = null,
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var amountText by remember { mutableStateOf(initialAmount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(if (initialAmount != null) "Tagihan" else "Catatan") }

    val categories = listOf("Tagihan", "Catatan", "Janji/Tugas", "Belanja")

    // Days offset: 0 = hari ini, 1 = besok, 3 = 3 hari lagi, 7 = minggu depan
    var selectedDaysOffset by remember { mutableStateOf(1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .imePadding(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tambah Pengingat Baru",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Judul
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Pengingat") },
                    placeholder = { Text("Misal: Bayar WiFi Indihome") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Catatan Detail
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Keterangan Tambahan (Opsional)") },
                    placeholder = { Text("Nomor pelanggan, lokasi, dll.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Nominal (Jika ada tagihan)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        amountText = digits
                        if (digits.isNotEmpty()) {
                            selectedCategory = "Tagihan"
                        }
                    },
                    label = { Text("Nominal Tagihan (Rp - Opsional)") },
                    placeholder = { Text("150000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        val parsed = amountText.toLongOrNull()
                        if (parsed != null && parsed > 0) {
                            Text(
                                text = "Terformat: ${BillExtractor.formatRupiah(parsed)}",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Kategori Selector Chips
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pengingat Jatuh Tempo
                Text(
                    text = "Waktu Pengingat",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val offsets = listOf(0 to "Hari ini", 1 to "Besok", 3 to "3 Hari", 7 to "1 Minggu")
                    offsets.forEach { (days, label) ->
                        val isSelected = selectedDaysOffset == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedDaysOffset = days }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol Simpan
                Button(
                    onClick = {
                        if (title.isBlank()) return@Button

                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, selectedDaysOffset)
                        cal.set(Calendar.HOUR_OF_DAY, 20)
                        cal.set(Calendar.MINUTE, 0)
                        val dueDate = cal.timeInMillis

                        val item = ReminderItem(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            content = content.trim(),
                            type = initialType,
                            amount = amountText.toLongOrNull(),
                            dueDate = dueDate,
                            createdAt = System.currentTimeMillis(),
                            isCompleted = false,
                            category = selectedCategory,
                            imageUri = initialImageUri,
                            audioPath = initialAudioPath
                        )
                        onSave(item)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = title.isNotBlank()
                ) {
                    Text(
                        text = "Simpan Pengingat",
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
