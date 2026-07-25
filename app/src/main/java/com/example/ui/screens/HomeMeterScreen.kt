package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.TripEntity
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.TripMapView
import com.example.viewmodel.MeterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMeterScreen(
    viewModel: MeterViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToReceipt: (Int) -> Unit
) {
    val context = LocalContext.current
    val tripState by viewModel.tripState.collectAsStateWithLifecycle()
    val allTrips by viewModel.allTrips.collectAsStateWithLifecycle()
    val hasBackup by viewModel.hasActiveSessionBackup.collectAsStateWithLifecycle()

    // Permission handling states
    var locationPermissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    // Register permissions on initial load
    LaunchedEffect(key1 = true) {
        viewModel.checkForActiveBackup()
        
        val required = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val missing = required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missing.isNotEmpty()) {
            launcher.launch(required.toTypedArray())
        }
    }

    // Automatically navigate to receipt if a trip finishes
    // Wait, the Stop button stops the service and saves the trip. Let's make sure we find the newly added trip ID to go there.
    // We can also let the user click on any past trip below to see its details.

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Get Taxi Meter",
                            color = Color(0xFF0F172A), // Slate-900
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .testTag("settings_button")
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF475569) // Slate-600
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFDFBFF) // Artistic Flair Off-White Light background
                )
            )
        },
        containerColor = Color(0xFFFDFBFF) // Creative light background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Backup Active Session Card in Soft Red
            if (hasBackup && tripState.status == TripStatus.IDLE) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF2F2) // Soft Red bg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore Active Session",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Unfinished Ride Detected",
                            color = Color(0xFF991B1B), // Red-800
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Your last tracking session was interrupted. Would you like to recover and resume?",
                            color = Color(0xFFB91C1C), // Red-700
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.discardBackup() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)), // Slate-200
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Discard", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.recoverTrip() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)), // Accent Red
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Resume Ride", color = Color.White, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            // Beautiful status & GPS diagnostics indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Status Column
                Column {
                    Text(
                        text = "DEVICE STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8), // slate-400
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (tripState.latitude != null) Color(0xFF22C55E) else Color(0xFFEF4444),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (tripState.latitude != null) "GPS Signal Strong" else "Acquiring GPS...",
                            color = Color(0xFF334155), // slate-700
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Meter Hired state badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (tripState.status) {
                                TripStatus.RUNNING -> Color(0xFFFEF2F2) // Soft red
                                TripStatus.PAUSED -> Color(0xFFFEF3C7) // Soft amber
                                else -> Color(0xFFF1F5F9) // Soft slate
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when (tripState.status) {
                                TripStatus.RUNNING -> Color(0xFFFEE2E2)
                                TripStatus.PAUSED -> Color(0xFFFEF3C7)
                                else -> Color(0xFFE2E8F0)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (tripState.status) {
                            TripStatus.RUNNING -> "Hired"
                            TripStatus.PAUSED -> "Paused"
                            else -> "Vacant"
                        },
                        color = when (tripState.status) {
                            TripStatus.RUNNING -> Color(0xFFDC2626) // Deep red
                            TripStatus.PAUSED -> Color(0xFFD97706) // Deep amber
                            else -> Color(0xFF475569) // slate-600
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }

            // HEADER ROUTE MAP
            TripMapView(
                tripState = tripState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(vertical = 4.dp)
            )

            // GIGANTIC FARE GAUGE DISPLAY CARD
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(32.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "CURRENT FARE",
                        color = Color(0xFF94A3B8), // slate-400
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 2.5.sp
                    )
                    
                    // Giant fare digit alongside superscript red symbol
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = String.format(Locale.US, "%.2f", tripState.currentFare),
                                color = Color(0xFF0F172A), // Dark slate font-black
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.testTag("fare_text")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tripState.currency,
                                color = Color(0xFFE53935), // Red Hot Accent Spec
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Grid of Distance & Wait time in clean light slate blocks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Distance Column box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("DISTANCE", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f", tripState.distanceKm),
                                        color = Color(0xFF1E293B),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("KM", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Wait Time Column box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(24.dp))
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WAIT TIME", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    val formattedWait = formatDuration(tripState.waitingSeconds).substring(3) // MM:SS
                                    Text(
                                        text = formattedWait,
                                        color = Color(0xFF1E293B),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("MIN", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // PRIMARY ACTION BUTTONS (START METER / PAUSE / END TRIP)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(vertical = 4.dp)
            ) {
                AnimatedContent(
                    targetState = tripState.status,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    },
                    label = "MainActionControls"
                ) { status ->
                    when (status) {
                        TripStatus.IDLE, TripStatus.FINISHED -> {
                            Button(
                                onClick = { viewModel.startTrip() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935), // Accent Red
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(32.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("start_trip_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "START METERED RIDE", 
                                        fontSize = 16.sp, 
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                            }
                        }

                        TripStatus.RUNNING -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.pauseTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(32.dp),
                                    border = BorderStroke(2.dp, Color(0xFFE53935)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Pause, contentDescription = null, tint = Color(0xFFE53935))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PAUSE", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), letterSpacing = 1.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.stopTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(32.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .fillMaxHeight()
                                        .testTag("stop_trip_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("END TRIP", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }

                        TripStatus.PAUSED -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resumeTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(32.dp),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .fillMaxHeight()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("RESUME", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.stopTrip() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                    shape = RoundedCornerShape(32.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("END TRIP", fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RIDE TIMINGS PANEL (White stylish card)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ELAPSED RIDE DURATION",
                            color = Color(0xFF94A3B8), // slate-400
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )
                        Text(
                            text = formatDuration(tripState.durationSeconds),
                            color = Color(0xFF0F172A),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Rotating indicator / dynamic icon if active
                    if (tripState.status == TripStatus.RUNNING) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEF2F2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Active ride indicator",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TRIP HISTORIES HEADER
            Text(
                text = "RECENT TRACKED TRIPS",
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // TRIPS HISTORY LIST
            if (allTrips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color(0xFFCBD5E1), // slate-300
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recorded trips yet.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trips_history_list")
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    allTrips.forEach { trip ->
                        HistoryTripRow(
                            trip = trip,
                            currency = tripState.currency,
                            onRowClick = { onNavigateToReceipt(trip.id) },
                            onDelete = { viewModel.deleteTrip(trip.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTripRow(
    trip: TripEntity,
    currency: String,
    onRowClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(trip.startTime))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() }
            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    color = Color(0xFF64748B), // slate-500
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", trip.distanceKm)} KM",
                        color = Color(0xFF1E293B), // slate-800
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s",
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currency${String.format(Locale.US, "%.2f", trip.totalFare)}",
                    color = Color(0xFFE53935), // Accent Red spec
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(end = 6.dp)
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = Color(0xFF94A3B8), // slate-400
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatDuration(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
}
