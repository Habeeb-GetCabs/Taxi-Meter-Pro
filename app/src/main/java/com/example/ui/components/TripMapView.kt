package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.TripState
import com.example.data.model.TripStatus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Check if MAPS_API_KEY is configured with a real key or placeholder
    val mapsApiKey = try { BuildConfig.MAPS_API_KEY } catch (e: Exception) { "" }
    val isKeyConfigured = mapsApiKey.isNotBlank() &&
            !mapsApiKey.contains("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("AIzaSyB7j5s8T369OKe4H69e3jhkGfM2sJhniCo", ignoreCase = true)

    var forceVectorCanvas by remember { mutableStateOf(!isKeyConfigured) }

    // Default location (e.g., San Francisco center) if GPS is null
    val currentLatLng = if (tripState.latitude != null && tripState.longitude != null) {
        LatLng(tripState.latitude, tripState.longitude)
    } else {
        LatLng(37.7749, -122.4194)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 16f)
    }

    // Auto-update camera on location change if user location moves
    LaunchedEffect(currentLatLng) {
        if (tripState.latitude != null && tripState.longitude != null) {
            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLng(currentLatLng)
                )
            } catch (_: Exception) {}
        }
    }

    val latLngRoute = remember(tripState.routePoints) {
        tripState.routePoints.map { LatLng(it.first, it.second) }
    }

    val startLatLng = remember(latLngRoute) {
        latLngRoute.firstOrNull()
    }

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false,
            mapType = MapType.NORMAL
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Modern slate-900 base
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!forceVectorCanvas && isKeyConfigured) {
                // Render standard Google Maps SDK view
                GoogleMap(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("google_trip_map"),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = uiSettings
                ) {
                    if (latLngRoute.size >= 2) {
                        Polyline(
                            points = latLngRoute,
                            color = Color(0xFFE53935),
                            width = 12f,
                            geodesic = true
                        )
                    }

                    if (startLatLng != null) {
                        Marker(
                            state = rememberMarkerState(position = startLatLng),
                            title = "Pick-up Location",
                            snippet = "Trip started here",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        )
                    }

                    if (tripState.latitude != null && tripState.longitude != null) {
                        Marker(
                            state = rememberMarkerState(position = currentLatLng),
                            title = "Taxi Position",
                            snippet = "Speed: ${String.format(Locale.US, "%.1f", tripState.speedKmH)} km/h",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }
                }
            } else {
                // High-contrast Real-Time Vector Route Canvas fallback
                GpsVectorRouteCanvas(
                    tripState = tripState,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Map Header & Key Status Badge Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (tripState.status == TripStatus.RUNNING) Color(0xFF22C55E) else Color(0xFF3B82F6)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (forceVectorCanvas) "GPS VECTOR MAP" else "GOOGLE MAPS LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (tripState.routePoints.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${tripState.routePoints.size} pts)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Mode Toggle Switch Button
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.88f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { forceVectorCanvas = !forceVectorCanvas }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (forceVectorCanvas) Icons.Default.Map else Icons.Default.Layers,
                                contentDescription = "Toggle Map Mode",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (forceVectorCanvas) "Google View" else "Vector View",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // API Key Notice Banner if default key is detected
                if (!isKeyConfigured) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF7C2D12).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFDBA74),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add MAPS_API_KEY in Secrets panel for Google satellite tiles",
                                fontSize = 10.sp,
                                color = Color(0xFFFFEDD5),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Recenter Camera Floating Button
            if (!forceVectorCanvas && isKeyConfigured) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                if (latLngRoute.size >= 2) {
                                    val builder = LatLngBounds.Builder()
                                    latLngRoute.forEach { builder.include(it) }
                                    val bounds = builder.build()
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngBounds(bounds, 64)
                                    )
                                } else {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)
                                    )
                                }
                            } catch (_: Exception) {}
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = Color(0xFFE53935),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(40.dp)
                        .align(Alignment.BottomEnd)
                        .testTag("map_recenter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Recenter Map",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GpsVectorRouteCanvas(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    val route = tripState.routePoints

    Canvas(modifier = modifier.background(Color(0xFF0F172A))) {
        val width = size.width
        val height = size.height

        // Draw grid lines for high-tech radar aesthetic
        val gridStep = 40.dp.toPx()
        var x = 0f
        while (x < width) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridStep
        }
        var y = 0f
        while (y < height) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridStep
        }

        if (route.isEmpty()) {
            // Draw idle pulse target at center
            val centerX = width / 2f
            val centerY = height / 2f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                radius = 36.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 8.dp.toPx(),
                center = Offset(centerX, centerY)
            )
            return@Canvas
        }

        // Calculate bounds to scale GPS route to canvas
        val lats = route.map { it.first }
        val lons = route.map { it.second }
        val minLat = lats.minOrNull() ?: 0.0
        val maxLat = lats.maxOrNull() ?: 0.0
        val minLon = lons.minOrNull() ?: 0.0
        val maxLon = lons.maxOrNull() ?: 0.0

        val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
        val lonRange = (maxLon - minLon).coerceAtLeast(0.0001)

        val padding = 48.dp.toPx()
        val usableW = width - (padding * 2)
        val usableH = height - (padding * 2)

        fun mapToScreen(lat: Double, lon: Double): Offset {
            val normX = ((lon - minLon) / lonRange).toFloat()
            val normY = (1.0f - ((lat - minLat) / latRange)).toFloat() // invert Y for screen space
            return Offset(
                x = padding + (normX * usableW),
                y = padding + (normY * usableH)
            )
        }

        // Draw Route Polyline
        val path = Path()
        val firstPoint = mapToScreen(route.first().first, route.first().second)
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until route.size) {
            val pt = mapToScreen(route[i].first, route[i].second)
            path.lineTo(pt.x, pt.y)
        }

        drawPath(
            path = path,
            color = Color(0xFFEF4444), // Vibrant Red route path
            style = Stroke(
                width = 8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw Pick-up Start Pin (Green)
        val startScreen = mapToScreen(route.first().first, route.first().second)
        drawCircle(
            color = Color(0xFF22C55E),
            radius = 10.dp.toPx(),
            center = startScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = startScreen
        )

        // Draw Current Vehicle Position Pin (Red / Pulse)
        val currentScreen = mapToScreen(route.last().first, route.last().second)
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = 0.3f),
            radius = 18.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = 10.dp.toPx(),
            center = currentScreen
        )
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = currentScreen
        )
    }
}

