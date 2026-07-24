package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.TripEntity
import com.example.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripReceiptScreen(
    viewModel: MeterViewModel,
    tripId: Int,
    currencySymbol: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val trip = remember(allTrips, tripId) { allTrips.find { it.id == tripId } }

    var passengerNameInput by remember { mutableStateOf("") }
    var noteStatusMessage by remember { mutableStateOf("") }

    // Synchronize passenger name note state input fields
    LaunchedEffect(trip) {
        if (trip != null) {
            passengerNameInput = trip.passengerNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Receipt", color = Color(0xFF0F172A), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("receipt_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Receipt session not found.", color = Color(0xFF64748B))
            }
        } else {
            val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
            val formattedDate = formatter.format(Date(trip.startTime))
            val durationMin = trip.durationSeconds / 60
            val durationSec = trip.durationSeconds % 60
            val waitingMin = trip.waitingSeconds / 60
            val waitingSec = trip.waitingSeconds % 60

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TICKET SHAPED PAPER CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBF9)), // Bone-white paper color
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Brand name
                        Text(
                            text = "TAXICAB INVOICE",
                            color = Color(0xFF0F172A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.8.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "PROFESSIONAL METER SYSTEMS",
                            color = Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Dotted Separator
                        DottedDivider(color = Color(0xFFCBD5E1))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date and ID lines
                        ReceiptLineItem(label = "Date / Time", value = formattedDate)
                        ReceiptLineItem(label = "Receipt Reference", value = "#${String.format(Locale.US, "%05d", trip.id)}")

                        if (trip.passengerNotes.isNotEmpty()) {
                            ReceiptLineItem(label = "Passenger Note", value = trip.passengerNotes)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(16.dp))

                        // TELEMETRY SUMMARY
                        ReceiptLineItem(label = "Driving Distance", value = "${String.format(Locale.US, "%.2f", trip.distanceKm)} km")
                        ReceiptLineItem(label = "Ride Duration", value = "${durationMin}m ${durationSec}s")
                        ReceiptLineItem(label = "Waiting Interval", value = "${waitingMin}m ${waitingSec}s")

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(16.dp))

                        // BROKEN DOWN BILLING ITEMS
                        Text(
                            text = "CHARGES BREAKDOWN",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ReceiptLineItem(label = "Flag Drop (Base Fare)", value = "$currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare - (trip.distanceKm * 2.00) - (waitingMin * 0.50))}")
                        ReceiptLineItem(label = "Distance driven", value = "$currencySymbol${String.format(Locale.US, "%.2f", trip.distanceKm * 2.00)}")
                        ReceiptLineItem(label = "Waiting charges", value = "$currencySymbol${String.format(Locale.US, "%.2f", waitingMin * 0.50)}")

                        Spacer(modifier = Modifier.height(16.dp))
                        DottedDivider(color = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.height(16.dp))

                        // TOTAL FARE BIG VALUE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "TOTAL FARE",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare)}",
                                color = Color(0xFFE53935), // Red Hot invoice finish
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("receipt_fare_display")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Thank you for riding with us!",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    }
                }

                // PASSENGER NAME NOTE SAVING CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Add passenger details / notes",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Associate an account ID, reference code, or passenger name.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passengerNameInput,
                            onValueChange = { passengerNameInput = it },
                            placeholder = { Text("Passenger name or account code") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                cursorColor = Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receipt_passenger_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Update trip database entity in Room with the note
                                coroutineScope.launch {
                                    val db = com.example.data.database.TripDatabase.getDatabase(context)
                                    db.tripDao().insertTrip(trip.copy(passengerNotes = passengerNameInput))
                                    noteStatusMessage = "Note saved!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, Color(0xFFE53935)), // Accent Red border
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }

                        if (noteStatusMessage.isNotEmpty()) {
                            Text(
                                text = noteStatusMessage,
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // SHARE RECEIPT BUTTON (Android Share Intent)
                Button(
                    onClick = { shareReceiptNative(context, trip, currencySymbol) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("share_receipt_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE RECEIPT REPORT", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ReceiptLineItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = Color(0xFF1C1C1C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DottedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

private fun shareReceiptNative(context: Context, trip: TripEntity, currency: String) {
    val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateText = formatter.format(Date(trip.startTime))
    val passenger = if (trip.passengerNotes.isNotEmpty()) "\nPassenger: ${trip.passengerNotes}" else ""
    val text = """
        === TAXI RIDE RECEIPT ===
        Date: $dateText$passenger
        Distance traveled: ${String.format(Locale.US, "%.2f", trip.distanceKm)} km
        Elapsed duration: ${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s
        Waiting Interval: ${trip.waitingSeconds / 60}m ${trip.waitingSeconds % 60}s
        --------------------------
        TOTAL CHARGE: $currency${String.format(Locale.US, "%.2f", trip.totalFare)}
        ==========================
        Thank you for choosing reliable rides!
    """.trimIndent()

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Taxi Invoice Receipt")
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(shareIntent)
}
