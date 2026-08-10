package com.deliriousvoid.openvkmatcha.ui.screens.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLat: Double? = null,
    initialLon: Double? = null,
    onBack: () -> Unit,
    onConfirm: (lat: Double, lon: Double, address: String?) -> Unit
) {
    val context = LocalContext.current
    var selectedLocation by remember {
        mutableStateOf(
            if (initialLat != null && initialLon != null) GeoPoint(initialLat, initialLon) else null
        )
    }
    var currentAddress by remember { mutableStateOf<String?>(null) }
    
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            if (selectedLocation != null) {
                controller.setCenter(selectedLocation)
            }
        }
    }

    LaunchedEffect(selectedLocation) {
        selectedLocation?.let { point ->
            withContext(Dispatchers.IO) {
                currentAddress = getAddress(context, point.latitude, point.longitude)
            }
        }
    }

    val locationOverlay = remember {
        MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
            enableMyLocation()
        }
    }

    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            if (selectedLocation != null) {
                position = selectedLocation
                mapView.overlays.add(this)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            locationOverlay.enableMyLocation()
            if (selectedLocation == null) {
                getCurrentLocation(context) { point ->
                    selectedLocation = point
                    marker.position = point
                    if (!mapView.overlays.contains(marker)) {
                        mapView.overlays.add(marker)
                    }
                    mapView.controller.animateTo(point)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        mapView.overlays.add(locationOverlay)
        
        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                selectedLocation = p
                marker.position = p
                if (!mapView.overlays.contains(marker)) {
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        mapView.overlays.add(MapEventsOverlay(eventsReceiver))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Выберите место", style = MaterialTheme.typography.titleMedium)
                        currentAddress?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Отмена")
                    }
                },
                actions = {
                    if (selectedLocation != null) {
                        IconButton(onClick = {
                            val loc = selectedLocation!!
                            onConfirm(loc.latitude, loc.longitude, currentAddress)
                        }) {
                            Icon(Icons.Default.Check, "Подтвердить")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                getCurrentLocation(context) { point ->
                    mapView.controller.animateTo(point)
                    selectedLocation = point
                    marker.position = point
                    if (!mapView.overlays.contains(marker)) {
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                }
            }) {
                Icon(Icons.Default.MyLocation, "Моё местоположение")
            }
        }
    ) { padding ->
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            update = { 
                // Any updates to the view if needed
            },
            onRelease = {
                it.onDetach()
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocation: (GeoPoint) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            onLocation(GeoPoint(location.latitude, location.longitude))
        }
    }
}

private fun getAddress(context: Context, lat: Double, lon: Double): String? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Geocoder.getFromLocation(Double, Double, Int, Geocoder.GeocodeListener) is available from API 33
            // But we can't easily use the callback here without more complexity.
            // Using the synchronous version for simplicity if possible, or just the old one.
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { address ->
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.toString().trim()
            }
        } else {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            addresses?.firstOrNull()?.let { address ->
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.toString().trim()
            }
        }
    } catch (e: Exception) {
        null
    }
}
