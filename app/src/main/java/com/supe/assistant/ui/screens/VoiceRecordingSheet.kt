package com.supe.assistant.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.supe.assistant.data.model.ReminderItem
import com.supe.assistant.data.model.ReminderType
import com.supe.assistant.data.parser.BillExtractor
import com.supe.assistant.ui.components.VoiceWaveVisualizer
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun VoiceRecordingSheet(
    onDismiss: () -> Unit,
    onSaveReminder: (ReminderItem) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var speechStatus by remember { mutableStateOf("Ketuk mikrofon dan mulai berbicara...") }

    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    speechStatus = "Mendengarkan suara Anda..."
                    isListening = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    speechStatus = "Memproses ucapan..."
                    isListening = false
                }
                override fun onError(error: Int) {
                    isListening = false
                    speechStatus = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara terdeteksi. Silakan coba lagi."
                        SpeechRecognizer.ERROR_NETWORK -> "Koneksi pengenalan suara terputus."
                        else -> "Tekan mic untuk berbicara."
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedText = matches[0]
                        speechStatus = "Suara berhasil dikenali."
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedText = matches[0]
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            recognizer = sr
        } else {
            speechStatus = "Pengenal suara sistem tidak tersedia di perangkat ini."
        }

        onDispose {
            recognizer?.destroy()
        }
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("id", "ID"))
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer?.startListening(intent)
            isListening = true
            speechStatus = "Mendengarkan..."
        }
    }

    fun stopListening() {
        recognizer?.stopListening()
        isListening = false
    }

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
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Input Suara Cepat",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visualizer
                VoiceWaveVisualizer(
                    isRecording = isListening,
                    modifier = Modifier.height(60.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = speechStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Mic Bundar Besar
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable {
                            if (isListening) stopListening() else startListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Perekam Suara",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Editable Text Field untuk hasil ucapan
                OutlinedTextField(
                    value = recognizedText,
                    onValueChange = { recognizedText = it },
                    label = { Text("Hasil Ucapan / Catatan Suara") },
                    placeholder = { Text("Contoh: Ingatkan bayar listrik 200 ribu besok malam") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // Bill Detection Preview
                val extracted = BillExtractor.parse(recognizedText)
                if (extracted.isBill && (extracted.amount != null || extracted.dueDate != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "💡 Terdeteksi sebagai Tagihan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (extracted.amount != null) {
                                Text(
                                    text = "Nominal: ${BillExtractor.formatRupiah(extracted.amount)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (recognizedText.isNotBlank()) {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            cal.set(Calendar.HOUR_OF_DAY, 20)
                            cal.set(Calendar.MINUTE, 0)

                            val finalDueDate = extracted.dueDate ?: cal.timeInMillis

                            val reminder = ReminderItem(
                                id = UUID.randomUUID().toString(),
                                title = recognizedText.take(45).trim(),
                                content = recognizedText.trim(),
                                type = ReminderType.VOICE,
                                amount = extracted.amount,
                                dueDate = finalDueDate,
                                createdAt = System.currentTimeMillis(),
                                isCompleted = false,
                                category = if (extracted.isBill) "Tagihan" else "Catatan"
                            )
                            onSaveReminder(reminder)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = recognizedText.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Simpan Catatan Suara", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
