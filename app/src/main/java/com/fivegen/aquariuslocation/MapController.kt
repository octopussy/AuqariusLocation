package com.fivegen.aquariuslocation

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapController(
    private val mapView: MapView,
    private val locationsSource: Flow<List<AppLocation>>,
    private val scope: CoroutineScope
) : CoroutineScope by scope {

    private var firstUpdate = true

    private val pathOverlay = Polyline(mapView)
    private val positionMarker = Marker(mapView)

    init {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.overlays.add(pathOverlay)
        mapView.overlays.add(positionMarker)
        launch {
            locationsSource.collect { list ->
                updateMarkers(list)
            }
        }
    }

    private fun updateMarkers(list: List<AppLocation>) {
        if (list.isEmpty()) return
        if (firstUpdate) {
            firstUpdate = false
            mapView.controller.animateTo(list.last().toGeo(), 19.0, 10)
        }

        pathOverlay.setPoints(list.map { it.toGeo() })
        positionMarker.position = list.last().toGeo()
    }
}

private fun AppLocation.toGeo(): GeoPoint {
    return GeoPoint(this.latitude, this.longitude, this.altitude)
}
