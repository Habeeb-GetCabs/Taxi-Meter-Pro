package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.LocationTrackingService
import com.example.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val tripState by LocationTrackingService.tripState.collectAsStateWithLifecycle()
    val baseFareState by viewModel.baseFare.collectAsStateWithLifecycle()
    val farePerKmState by viewModel.farePerKm.collectAsStateWithLifecycle()
    val waitFarePerMinState by viewModel.waitFarePerMin.collectAsStateWithLifecycle()
    val speedThresholdState by viewModel.speedThreshold.collectAsStateWithLifecycle()
    val audioEnabledState by viewModel.audioEnabled.collectAsStateWithLifecycle()
    val autoStartEnabledState by viewModel.autoStartEnabled.collectAsStateWithLifecycle()
    val currencyState by viewModel.currency.collectAsStateWithLifecycle()
    val outOfCitySurchargePercentState by viewModel.outOfCitySurchargePercent.collectAsStateWithLifecycle()

    var baseFareInput by remember { mutableStateOf("") }
    var farePerKmInput by remember { mutableStateOf("") }
    var waitFarePerMinInput by remember { mutableStateOf("") }
    var speedThresholdInput by remember { mutableStateOf("") }
    var currencyInput by remember { mutableStateOf("") }
    var outOfCitySurchargePercentInput by remember { mutableStateOf("") }
    var audioEnabled by remember { mutableStateOf(true) }
    var autoStartEnabled by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Synchronize inputs once preferences load
    LaunchedEffect(baseFareState, farePerKmState, waitFarePerMinState, speedThresholdState, currencyState, outOfCitySurchargePercentState, audioEnabledState, autoStartEnabledState) {
        baseFareInput = baseFareState.toString()
        farePerKmInput = farePerKmState.toString()
        waitFarePerMinInput = waitFarePerMinState.toString()
        speedThresholdInput = speedThresholdState.toString()
        currencyInput = currencyState
        outOfCitySurchargePercentInput = outOfCitySurchargePercentState.toString()
        audioEnabled = audioEnabledState
        autoStartEnabled = autoStartEnabledState
    }

    var showSavedMessage by remember { mutableStateOf(false) }

    // Live Calculator Preview values
    val currentBf = baseFareInput.toDoubleOrNull() ?: 0.0
    val currentFk = farePerKmInput.toDoubleOrNull() ?: 0.0
    val currentWf = waitFarePerMinInput.toDoubleOrNull() ?: 0.0
    val sampleKm = 5.0
    val sampleWaitMin = 3.0
    val sampleTotalFare = currentBf + (sampleKm * currentFk) + (sampleWaitMin * currentWf)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fare Engine Config", color = Color(0xFF0F172A), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
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
        containerColor = Color(0xFFFDFBFF),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val bf = baseFareInput.toDoubleOrNull() ?: baseFareState
                    val fk = farePerKmInput.toDoubleOrNull() ?: farePerKmState
                    val wf = waitFarePerMinInput.toDoubleOrNull() ?: waitFarePerMinState
                    val st = speedThresholdInput.toDoubleOrNull() ?: speedThresholdState
                    val ooc = outOfCitySurchargePercentInput.toDoubleOrNull() ?: outOfCitySurchargePercentState
                    
                    viewModel.updateBaseFare(bf)
                    viewModel.updateFarePerKm(fk)
                    viewModel.updateWaitFarePerMin(wf)
                    viewModel.updateSpeedThreshold(st)
                    viewModel.updateCurrency(currencyInput)
                    viewModel.updateOutOfCitySurchargePercent(ooc)
                    viewModel.updateAudioEnabled(audioEnabled)
                    viewModel.updateAutoStartEnabled(autoStartEnabled)

                    com.example.service.LocationTrackingService.startMonitoring(
                        context = context,
                        baseFare = bf,
                        farePerKm = fk,
                        waitFarePerMin = wf,
                        currency = currencyInput,
                        speedThreshold = st,
                        autoStartEnabled = autoStartEnabled
                    )

                    showSavedMessage = true
                },
                icon = { Icon(Icons.Default.Save, contentDescription = null, tint = Color.White) },
                text = { Text("Save Config", fontWeight = FontWeight.Black, letterSpacing = 1.sp) },
                containerColor = Color(0xFFE53935), // Artistic Accent Red
                contentColor = Color.White,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.testTag("save_settings_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            if (showSavedMessage) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)), // Soft emerald green
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFA7F3D0), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF065F46))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Fare Engine Configuration Saved & Calculator Updated!", color = Color(0xFF065F46), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }

            // QUICK PRESET SELECTION CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TARIFF PRESETS",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currencyInput == "₹" && baseFareInput == "80.0",
                            onClick = {
                                baseFareInput = "80.0"
                                farePerKmInput = "28.0"
                                waitFarePerMinInput = "2.0"
                                speedThresholdInput = "5.0"
                                currencyInput = "₹"
                            },
                            label = { Text("🚕 Standard Taxi (₹80/₹28)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDCFCE7),
                                selectedLabelColor = Color(0xFF15803D)
                            )
                        )
                    }
                }
            }

            // LIVE CALCULATOR PREVIEW CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // Dark slate canvas
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE CALCULATOR PREVIEW",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ESTIMATOR",
                                color = Color(0xFF86EFAC),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sample Trip: 5.0 km distance + 3 mins wait",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• Base Fare: $currencyInput${String.format(java.util.Locale.US, "%.2f", currentBf)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Dist: $currencyInput${String.format(java.util.Locale.US, "%.2f", sampleKm * currentFk)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "• Wait: $currencyInput${String.format(java.util.Locale.US, "%.2f", sampleWaitMin * currentWf)}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp
                        )
                    }

                    Divider(
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Total Fare:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currencyInput${String.format(java.util.Locale.US, "%.2f", sampleTotalFare)}",
                            color = Color(0xFF4ADE80), // Emerald 400
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Text(
                text = "CUSTOM FARE METRICS",
                color = Color(0xFF94A3B8), // slate-400
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.0.sp
            )

            // Base Fare Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Base Fare Setup ($currencyInput)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The initial flag drop charge applied when a trip starts.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = baseFareInput,
                        onValueChange = { baseFareInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            .testTag("input_base_fare")
                    )
                }
            }

            // Fare Per Kilometer Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Distance Rate ($currencyInput per KM)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The rate charged per physical kilometer driven during service.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = farePerKmInput,
                        onValueChange = { farePerKmInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            .testTag("input_fare_per_km")
                    )
                }
            }

            // Waiting Charge Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Wait Charges ($currencyInput per Minute)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "The charge accumulated per minute when speed is below threshold.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = waitFarePerMinInput,
                        onValueChange = { waitFarePerMinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            .testTag("input_wait_fare_per_min")
                    )
                }
            }

            // Speed Threshold Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Wait Speed Threshold (km/h)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Speed boundary (km/h) below which wait billing takes effect.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    OutlinedTextField(
                        value = speedThresholdInput,
                        onValueChange = { speedThresholdInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            .testTag("input_speed_threshold")
                    )
                }
            }

            // Out of City Surcharge & Mode Toggle Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (tripState.isOutOfCity) Color(0xFFFFF7ED) else Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = if (tripState.isOutOfCity) Color(0xFFEA580C) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (tripState.isOutOfCity) Color(0xFFFFEDD5) else Color(0xFFF1F5F9),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = if (tripState.isOutOfCity) Color(0xFFEA580C) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Out of City Charges Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (tripState.isOutOfCity) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ACTIVE",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color(0xFFEA580C),
                                            modifier = Modifier
                                                .background(Color(0xFFFFEDD5), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (tripState.isOutOfCity)
                                        "Outstation tariff active (+${outOfCitySurchargePercentInput}% surcharge)"
                                    else
                                        "Enable outstation return tariff for long distance rides",
                                    fontSize = 11.sp,
                                    color = if (tripState.isOutOfCity) Color(0xFFC2410C) else Color(0xFF64748B)
                                )
                            }
                        }

                        Switch(
                            checked = tripState.isOutOfCity,
                            onCheckedChange = { enabled ->
                                val surchargePct = outOfCitySurchargePercentInput.toDoubleOrNull() ?: outOfCitySurchargePercentState
                                LocationTrackingService.toggleOutOfCity(context, enabled, surchargePct)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEA580C),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.testTag("toggle_out_of_city")
                        )
                    }

                    HorizontalDivider(
                        color = if (tripState.isOutOfCity) Color(0xFFFED7AA) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Text(
                        text = "Out of City Surcharge Rate (%)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Percentage added to base + distance fare when Out of City mode is enabled.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    OutlinedTextField(
                        value = outOfCitySurchargePercentInput,
                        onValueChange = { outOfCitySurchargePercentInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEA580C),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            cursorColor = Color(0xFFEA580C)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_out_of_city_surcharge")
                    )
                }
            }

            // Currency Symbol Card with Indian Rupee (₹) and Other Symbol Chips
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Currency Symbol",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Select a preset symbol or type a custom currency sign.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("₹", "$", "€", "£", "¥").forEach { symbol ->
                            FilterChip(
                                selected = currencyInput == symbol,
                                onClick = { currencyInput = symbol },
                                label = { Text(symbol, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE53935),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currencyInput,
                        onValueChange = { currencyInput = it },
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
                            .testTag("input_currency")
                    )
                }
            }

            // Sound Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Start Meter on Movement",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Automatically starts or resumes the meter when vehicle speed exceeds the threshold.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = autoStartEnabled,
                        onCheckedChange = { autoStartEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE53935),
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.testTag("toggle_autostart")
                    )
                }
            }

            // Sound Toggle Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sound & TTS Announcements",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Enables voice assistant readouts for trip start, pause, and endpoints.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = audioEnabled,
                        onCheckedChange = { audioEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE53935), // Red Hot Accent Spec
                            uncheckedThumbColor = Color(0xFF94A3B8), // slate-400
                            uncheckedTrackColor = Color(0xFFE2E8F0) // slate-200
                        ),
                        modifier = Modifier.testTag("toggle_audio")
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Avoid hiding behind FAB
        }
    }
}
