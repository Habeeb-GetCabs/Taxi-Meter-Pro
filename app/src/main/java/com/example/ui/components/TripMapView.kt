package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.viewinterop.AndroidView
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

enum class MapTypeMode {
    OSM_FREE_STREET,
    OSM_FREE_DARK,
    VECTOR_RADAR,
    GOOGLE_MAPS
}

@Composable
fun TripMapView(
    tripState: TripState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Check if MAPS_API_KEY is configured with a real key
    val mapsApiKey = try { BuildConfig.MAPS_API_KEY } catch (e: Exception) { "" }
    val isGoogleKeyConfigured = mapsApiKey.isNotBlank() &&
            !mapsApiKey.contains("DEFAULT_MAPS_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true) &&
            !mapsApiKey.contains("AIzaSyB7j5s8T369OKe4H69e3jhkGfM2sJhniCo", ignoreCase = true)

    // Default map mode is 100% Free OpenStreetMap
    var selectedMode by remember { mutableStateOf(MapTypeMode.OSM_FREE_STREET) }

    // LatLng for Google Maps if used
    val currentLatLng = if (tripState.latitude != null && tripState.longitude != null) {
        LatLng(tripState.latitude, tripState.longitude)
    } else {
        LatLng(37.7749, -122.4194)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 16f)
    }

    LaunchedEffect(currentLatLng) {
        if (tripState.latitude != null && tripState.longitude != null && selectedMode == MapTypeMode.GOOGLE_MAPS) {
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLng(currentLatLng))
            } catch (_: Exception) {}
        }
    }

    val latLngRoute = remember(tripState.routePoints) {
        tripState.routePoints.map { LatLng(it.first, it.second) }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedMode) {
                MapTypeMode.OSM_FREE_STREET -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        useDarkStyle = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.OSM_FREE_DARK -> {
                    OpenStreetMapTileView(
                        tripState = tripState,
                        useDarkStyle = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.VECTOR_RADAR -> {
                    GpsVectorRouteCanvas(
                        tripState = tripState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MapTypeMode.GOOGLE_MAPS -> {
                    if (isGoogleKeyConfigured) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("google_trip_map"),
                            cameraPositionState = cameraPositionState
                        ) {
                            if (latLngRoute.size >= 2) {
                                Polyline(
                                    points = latLngRoute,
                                    color = Color(0xFFE53935),
                                    width = 12f
                                )
                            }
                            if (tripState.latitude != null && tripState.longitude != null) {
                                Marker(
                                    state = rememberMarkerState(position = currentLatLng),
                                    title = "Taxi Position",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                        }
                    } else {
                        OpenStreetMapTileView(
                            tripState = tripState,
                            useDarkStyle = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Map Overlay Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
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
                                text = when (selectedMode) {
                                    MapTypeMode.OSM_FREE_STREET -> "FREE OPENSTREETMAP"
                                    MapTypeMode.OSM_FREE_DARK -> "FREE DARK MAP"
                                    MapTypeMode.VECTOR_RADAR -> "VECTOR RADAR MAP"
                                    MapTypeMode.GOOGLE_MAPS -> "GOOGLE MAPS LIVE"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF166534),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "100% FREE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Map Mode Switcher Button
                    Surface(
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            selectedMode = when (selectedMode) {
                                MapTypeMode.OSM_FREE_STREET -> MapTypeMode.OSM_FREE_DARK
                                MapTypeMode.OSM_FREE_DARK -> MapTypeMode.VECTOR_RADAR
                                MapTypeMode.VECTOR_RADAR -> if (isGoogleKeyConfigured) MapTypeMode.GOOGLE_MAPS else MapTypeMode.OSM_FREE_STREET
                                MapTypeMode.GOOGLE_MAPS -> MapTypeMode.OSM_FREE_STREET
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Switch Map Style",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (selectedMode) {
                                    MapTypeMode.OSM_FREE_STREET -> "Street Mode"
                                    MapTypeMode.OSM_FREE_DARK -> "Dark Mode"
                                    MapTypeMode.VECTOR_RADAR -> "Vector Radar"
                                    MapTypeMode.GOOGLE_MAPS -> "Google View"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OpenStreetMapTileView(
    tripState: TripState,
    useDarkStyle: Boolean,
    modifier: Modifier = Modifier
) {
    val lat = tripState.latitude ?: 37.7749
    val lon = tripState.longitude ?: -122.4194
    val routePoints = tripState.routePoints

    val htmlContent = remember(useDarkStyle) {
        val tileUrl = if (useDarkStyle) {
            "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        } else {
            "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        }
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #0f172a; }
                .leaflet-control-attribution { font-size: 8px !important; opacity: 0.5; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false, attributionControl: false }).setView([$lat, $lon], 16);
                L.tileLayer('$tileUrl', {
                    maxZoom: 19,
                    attribution: 'OpenStreetMap'
                }).addTo(map);

                var startMarker = null;
                var currentMarker = null;
                var routePolyline = null;

                function updateLocation(currLat, currLon, routeJson) {
                    var latLng = [currLat, currLon];
                    
                    if (!currentMarker) {
                        var redIcon = L.divIcon({
                            className: 'custom-taxi-pin',
                            html: "<div style='background-color:#ef4444;width:18px;height:18px;border-radius:50%;border:3px solid #ffffff;box-shadow:0 0 12px rgba(239,68,68,0.9);'></div>",
                            iconSize: [18, 18],
                            iconAnchor: [9, 9]
                        });
                        currentMarker = L.marker(latLng, {icon: redIcon}).addTo(map);
                    } else {
                        currentMarker.setLatLng(latLng);
                    }
                    map.panTo(latLng);

                    try {
                        var points = JSON.parse(routeJson);
                        if (points && points.length > 0) {
                            if (!startMarker) {
                                var greenIcon = L.divIcon({
                                    className: 'custom-start-pin',
                                    html: "<div style='background-color:#22c55e;width:14px;height:14px;border-radius:50%;border:2px solid #ffffff;'></div>",
                                    iconSize: [14, 14],
                                    iconAnchor: [7, 7]
                                });
                                startMarker = L.marker([points[0][0], points[0][1]], {icon: greenIcon}).addTo(map);
                            }
                            if (routePolyline) {
                                routePolyline.setLatLngs(points);
                            } else {
                                routePolyline = L.polyline(points, {color: '#ef4444', weight: 5, opacity: 0.95}).addTo(map);
                            }
                        }
                    } catch(e){}
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        val routeJson = routePoints.joinToString(prefix = "[", postfix = "]") { "[${it.first},${it.second}]" }
                        evaluateJavascript("updateLocation($lat, $lon, '$routeJson');", null)
                    }
                }
                loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            val routeJson = routePoints.joinToString(prefix = "[", postfix = "]") { "[${it.first},${it.second}]" }
            webView.evaluateJavascript("updateLocation($lat, $lon, '$routeJson');", null)
        },
        modifier = modifier
    )
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
            val normY = (1.0f - ((lat - minLat) / latRange)).toFloat()
            return Offset(
                x = padding + (normX * usableW),
                y = padding + (normY * usableH)
            )
        }

        val path = Path()
        val firstPoint = mapToScreen(route.first().first, route.first().second)
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until route.size) {
            val pt = mapToScreen(route[i].first, route[i].second)
            path.lineTo(pt.x, pt.y)
        }

        drawPath(
            path = path,
            color = Color(0xFFEF4444),
            style = Stroke(
                width = 8f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

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
