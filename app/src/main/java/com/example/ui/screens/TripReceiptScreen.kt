package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
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
    val trip = remember(allTrips, tripId) { 
        if (tripId == 0 && allTrips.isNotEmpty()) allTrips.first() else allTrips.find { it.id == tripId } 
    }

    var customerPhoneInput by remember { mutableStateOf("9043743777") }
    var passengerNameInput by remember { mutableStateOf("") }
    var noteStatusMessage by remember { mutableStateOf("") }

    LaunchedEffect(trip) {
        if (trip != null && trip.passengerNotes.isNotEmpty()) {
            passengerNameInput = trip.passengerNotes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Taxi Receipt Bill", color = Color(0xFF0F172A), fontWeight = FontWeight.Black) },
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
            val dateOnlyFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
            val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
            
            val formattedDate = dateOnlyFormatter.format(Date(trip.startTime))
            val startTimeStr = timeFormatter.format(Date(trip.startTime))
            val endTimeStr = if (trip.endTime > 0) timeFormatter.format(Date(trip.endTime)) else timeFormatter.format(Date(trip.startTime + (trip.durationSeconds * 1000)))
            
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
                // ELEGANT OFFICIAL TAXI BILL CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBF9)), // Clean paper white
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header Logo Image
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Color(0xFFE53935), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(64.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "WELCOME TO GET TAXI METER",
                            color = Color(0xFFE53935),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = "TAXICAB OFFICIAL RECEIPT",
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Text(
                            text = "Ref #${String.format(Locale.US, "%05d", trip.id)} • $formattedDate",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                        )

                        // Dotted Divider
                        DottedDivider(color = Color(0xFFCBD5E1))

                        Spacer(modifier = Modifier.height(16.dp))

                        // TRIP TIMINGS & DISTANCE SECTION
                        Text(
                            text = "TRIP TIMING & METRICS",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ReceiptLineItem(label = "Start Time", value = startTimeStr)
                        ReceiptLineItem(label = "End Time", value = endTimeStr)
                        ReceiptLineItem(label = "Total Driving Distance", value = "${String.format(Locale.US, "%.2f", trip.distanceKm)} km")
                        ReceiptLineItem(label = "Elapsed Ride Duration", value = "${durationMin}m ${durationSec}s")
                        ReceiptLineItem(label = "Waiting Time", value = "${waitingMin}m ${waitingSec}s")

                        val pLoc = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else if (trip.startLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.startLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.startLongitude)}" else "GPS Location"
                        val dLoc = if (trip.dropAddress.isNotBlank()) trip.dropAddress else if (trip.endLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.endLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.endLongitude)}" else "GPS Location"

                        ReceiptLineItem(label = "📍 Pick up Location", value = pLoc)
                        ReceiptLineItem(label = "🏁 Drop Location", value = dLoc)

                        if (trip.startLatitude != 0.0 && trip.endLatitude != 0.0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        val mapsUrl = "https://www.google.com/maps/dir/?api=1&origin=${trip.startLatitude},${trip.startLongitude}&destination=${trip.endLatitude},${trip.endLongitude}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFE53935))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Route on Google Maps 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                                }
                            }
                        }

                        if (trip.passengerNotes.isNotEmpty()) {
                            ReceiptLineItem(label = "Passenger Notes", value = trip.passengerNotes)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        DottedDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(16.dp))

                        // ITEMIZED FARE CHARGES
                        Text(
                            text = "CHARGES BREAKDOWN",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Estimated breakdown values
                        val estBase = trip.baseFare
                        val estDistFare = trip.distanceKm * trip.farePerKm
                        val estWaitFare = (trip.waitingSeconds / 60.0) * trip.waitFarePerMin

                        ReceiptLineItem(label = "Base Fare (Minimum)", value = "$currencySymbol${String.format(Locale.US, "%.2f", estBase)}")
                        ReceiptLineItem(label = "Distance Fare (${String.format(Locale.US, "%.1f", trip.distanceKm)} km × $currencySymbol${String.format(Locale.US, "%.2f", trip.farePerKm)}/km)", value = "$currencySymbol${String.format(Locale.US, "%.2f", estDistFare)}")
                        ReceiptLineItem(label = "Waiting Charge (${waitingMin}m ${waitingSec}s × $currencySymbol${String.format(Locale.US, "%.2f", trip.waitFarePerMin)}/min)", value = "$currencySymbol${String.format(Locale.US, "%.2f", estWaitFare)}")

                        if (trip.isOutOfCity) {
                            ReceiptLineItem(
                                label = "Out of City Outstation Surcharge", 
                                value = "+$currencySymbol${String.format(Locale.US, "%.2f", trip.outOfCitySurcharge)}"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        DottedDivider(color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(16.dp))

                        // TOTAL FARE HIGHLIGHT
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL AMOUNT DUE",
                                    color = Color(0xFF0F172A),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "All Taxes & Charges Included",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%.2f", trip.totalFare)}",
                                color = Color(0xFFE53935),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("receipt_fare_display")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // COURTESY THANK YOU FOOTER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🙏 Thanks for riding with us! Have a wonderful & safe journey!",
                                color = Color(0xFF991B1B),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // DIRECT WHATSAPP BILL SENDING CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF25D366), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "WhatsApp",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Send Bill Directly to Customer",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Enter customer WhatsApp number to send invoice instantly.",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = customerPhoneInput,
                            onValueChange = { customerPhoneInput = it },
                            label = { Text("Customer Mobile / WhatsApp Number") },
                            placeholder = { Text("e.g. 9043743777") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF25D366)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF25D366),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                cursorColor = Color(0xFF25D366)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                sendBillViaWhatsApp(
                                    context = context,
                                    phone = customerPhoneInput,
                                    trip = trip,
                                    currency = currencySymbol,
                                    startTimeStr = startTimeStr,
                                    endTimeStr = endTimeStr,
                                    durationMin = durationMin,
                                    durationSec = durationSec,
                                    waitingMin = waitingMin,
                                    waitingSec = waitingSec
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "SEND BILL VIA WHATSAPP",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }

                // PASSENGER NOTES CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Passenger Name / Ref Notes",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passengerNameInput,
                            onValueChange = { passengerNameInput = it },
                            placeholder = { Text("e.g. John Doe / Booking #102") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFE53935),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val db = com.example.data.database.TripDatabase.getDatabase(context)
                                    db.tripDao().insertTrip(trip.copy(passengerNotes = passengerNameInput))
                                    noteStatusMessage = "Note saved successfully!"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, Color(0xFFE53935)),
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

                // GENERAL SHARE BUTTON
                Button(
                    onClick = {
                        shareReceiptNative(
                            context = context,
                            trip = trip,
                            currency = currencySymbol,
                            startTimeStr = startTimeStr,
                            endTimeStr = endTimeStr,
                            durationMin = durationMin,
                            durationSec = durationSec,
                            waitingMin = waitingMin,
                            waitingSec = waitingSec
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("share_receipt_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHARE RECEIPT TO OTHER APPS",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
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
        Text(text = label, color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

private fun buildBillTextMessage(
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
): String {
    val dateOnlyFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateOnlyFormatter.format(Date(trip.startTime))
    val passenger = if (trip.passengerNotes.isNotEmpty()) "\n👤 Passenger: ${trip.passengerNotes}" else ""

    val pLoc = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else if (trip.startLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.startLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.startLongitude)}" else "GPS Location"
    val dLoc = if (trip.dropAddress.isNotBlank()) trip.dropAddress else if (trip.endLatitude != 0.0) "Lat: ${String.format(Locale.US, "%.4f", trip.endLatitude)}, Lng: ${String.format(Locale.US, "%.4f", trip.endLongitude)}" else "GPS Location"

    val outOfCityText = if (trip.isOutOfCity) {
        "\n🛣️ Out of City Surcharge: $currency${String.format(Locale.US, "%.2f", trip.outOfCitySurcharge)}"
    } else ""

    val mapLinkText = if (trip.startLatitude != 0.0 && trip.endLatitude != 0.0) {
        "\n🗺️ Google Maps Route: https://www.google.com/maps/dir/?api=1&origin=${trip.startLatitude},${trip.startLongitude}&destination=${trip.endLatitude},${trip.endLongitude}"
    } else ""

    return """
🚕 *GET TAXI METER - OFFICIAL TRIP BILL* 🚕
WELCOME TO GET TAXI METER!

📅 Date: $dateStr
⏰ Start Time: $startTimeStr
⏰ End Time: $endTimeStr
⏱️ Ride Duration: ${durationMin}m ${durationSec}s
⏳ Waiting Time: ${waitingMin}m ${waitingSec}s
📍 Pick up Location: $pLoc
🏁 Drop Location: $dLoc
🛣️ Total Distance: ${String.format(Locale.US, "%.2f", trip.distanceKm)} km$passenger$mapLinkText

----------------------------------
💰 Base Fare: $currency${String.format(Locale.US, "%.2f", trip.baseFare)}
🛣️ Distance Charge: $currency${String.format(Locale.US, "%.2f", trip.distanceKm * trip.farePerKm)}
⌛ Wait Charge: $currency${String.format(Locale.US, "%.2f", (trip.waitingSeconds / 60.0) * trip.waitFarePerMin)}$outOfCityText
----------------------------------
💳 *TOTAL FARE DUE: $currency${String.format(Locale.US, "%.2f", trip.totalFare)}*
==================================

🙏 Thanks for riding with us! Have a safe & wonderful journey!
    """.trimIndent()
}

private fun sendBillViaWhatsApp(
    context: Context,
    phone: String,
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
) {
    val text = buildBillTextMessage(
        trip = trip,
        currency = currency,
        startTimeStr = startTimeStr,
        endTimeStr = endTimeStr,
        durationMin = durationMin,
        durationSec = durationSec,
        waitingMin = waitingMin,
        waitingSec = waitingSec
    )

    try {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val encodedText = Uri.encode(text)
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Receipt Bill"))
    }
}

private fun shareReceiptNative(
    context: Context,
    trip: TripEntity,
    currency: String,
    startTimeStr: String,
    endTimeStr: String,
    durationMin: Long,
    durationSec: Long,
    waitingMin: Long,
    waitingSec: Long
) {
    val text = buildBillTextMessage(
        trip = trip,
        currency = currency,
        startTimeStr = startTimeStr,
        endTimeStr = endTimeStr,
        durationMin = durationMin,
        durationSec = durationSec,
        waitingMin = waitingMin,
        waitingSec = waitingSec
    )

    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Taxi Invoice Receipt")
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(shareIntent)
}
