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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
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
import com.example.data.database.TripDatabase
import com.example.data.database.TripEntity
import com.example.viewmodel.MeterViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSummaryScreen(
    tripId: Int,
    viewModel: MeterViewModel? = null,
    currencySymbol: String = "$",
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Retrieve data using Room database directly or via ViewModel
    var trip by remember { mutableStateOf<TripEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch trip record from Room database
    LaunchedEffect(tripId) {
        isLoading = true
        val database = TripDatabase.getDatabase(context)
        trip = database.tripDao().getTripById(tripId)
        isLoading = false
    }

    var passengerNotesInput by remember { mutableStateOf("") }
    var noteSaveMessage by remember { mutableStateOf("") }

    LaunchedEffect(trip) {
        trip?.let {
            passengerNotesInput = it.passengerNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Trip Summary", 
                        color = Color(0xFF0F172A), 
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("summary_back_button")
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE53935))
            }
        } else if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Trip details not found in database.",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            val currentTrip = trip!!
            val dateFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
            val formattedDate = dateFormatter.format(Date(currentTrip.startTime))
            
            val durationMin = currentTrip.durationSeconds / 60
            val durationSec = currentTrip.durationSeconds % 60
            val waitMin = currentTrip.waitingSeconds / 60
            val waitSec = currentTrip.waitingSeconds % 60

            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO FARE DISPLAY CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CALCULATED TOTAL FARE",
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.0.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = String.format(Locale.US, "%.2f", currentTrip.totalFare),
                                color = Color(0xFF0F172A),
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("summary_fare_text")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currencySymbol,
                                color = Color(0xFFE53935),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = formattedDate,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // TELEMETRY SUMMARY GRID (Distance, Duration, Wait Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Distance Tile
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DISTANCE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.2f", currentTrip.distanceKm)} km",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("summary_distance_text")
                            )
                        }
                    }

                    // Duration Tile
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7), // Sky blue accent
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DURATION", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${durationMin}m ${durationSec}s",
                                color = Color(0xFF0F172A),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("summary_duration_text")
                            )
                        }
                    }
                }

                // DETAILED BILLING BREAKDOWN CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "CALCULATED FARE DETAILS",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SummaryLineRow(
                            label = "Trip Reference Code",
                            value = "#${String.format(Locale.US, "%05d", currentTrip.id)}"
                        )
                        SummaryLineRow(
                            label = "Driving Distance",
                            value = "${String.format(Locale.US, "%.2f", currentTrip.distanceKm)} km"
                        )
                        SummaryLineRow(
                            label = "Total Elapsed Time",
                            value = "${durationMin}m ${durationSec}s"
                        )
                        SummaryLineRow(
                            label = "Waiting / Idle Time",
                            value = "${waitMin}m ${waitSec}s"
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryDottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Final Calculated Charge",
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%.2f", currentTrip.totalFare)}",
                                color = Color(0xFFE53935),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // LOCATION DETAILS CARD (If coordinates logged)
                if (currentTrip.startLatitude != 0.0 || currentTrip.startLongitude != 0.0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ROUTE COORDINATES",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            SummaryLineRow(
                                label = "Origin GPS",
                                value = "${String.format(Locale.US, "%.4f", currentTrip.startLatitude)}, ${String.format(Locale.US, "%.4f", currentTrip.startLongitude)}"
                            )
                            SummaryLineRow(
                                label = "Destination GPS",
                                value = "${String.format(Locale.US, "%.4f", currentTrip.endLatitude)}, ${String.format(Locale.US, "%.4f", currentTrip.endLongitude)}"
                            )
                        }
                    }
                }

                // PASSENGER NOTES & ROOM UPDATE CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "PASSENGER NOTES & METADATA",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = passengerNotesInput,
                            onValueChange = { passengerNotesInput = it },
                            placeholder = { Text("Enter passenger name, account ref, or notes") },
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
                                .testTag("summary_passenger_notes_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val db = TripDatabase.getDatabase(context)
                                    val updatedTrip = currentTrip.copy(passengerNotes = passengerNotesInput)
                                    db.tripDao().insertTrip(updatedTrip)
                                    trip = updatedTrip
                                    noteSaveMessage = "Saved to Room database!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(2.dp, Color(0xFFE53935)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Note to Room", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }

                        if (noteSaveMessage.isNotEmpty()) {
                            Text(
                                text = noteSaveMessage,
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // SHARE SUMMARY ACTION BUTTON
                Button(
                    onClick = {
                        shareTripSummary(context, currentTrip, currencySymbol)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("share_summary_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE TRIP SUMMARY",
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
private fun SummaryLineRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryDottedDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )
    }
}

private fun shareTripSummary(context: Context, trip: TripEntity, currency: String) {
    val formatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateText = formatter.format(Date(trip.startTime))
    val passenger = if (trip.passengerNotes.isNotEmpty()) "\nPassenger/Notes: ${trip.passengerNotes}" else ""
    val text = """
        === COMPLETED TAXI TRIP SUMMARY ===
        Date: $dateText$passenger
        Total Distance: ${String.format(Locale.US, "%.2f", trip.distanceKm)} km
        Total Duration: ${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s
        Waiting Duration: ${trip.waitingSeconds / 60}m ${trip.waitingSeconds % 60}s
        -----------------------------------
        CALCULATED TOTAL FARE: $currency${String.format(Locale.US, "%.2f", trip.totalFare)}
        ===================================
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Completed Trip Summary")
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(shareIntent)
}
