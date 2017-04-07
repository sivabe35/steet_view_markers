package com.github.alkurop.streetviewmarker

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.FrameLayout
import com.github.alkurop.streetviewmarker.components.IStreetOverlayView
import com.github.alkurop.streetviewmarker.components.StreetOverlayView
import com.github.alkurop.streetviewmarker.components.TouchOverlayView
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.StreetViewPanoramaCamera
import java.util.*

/**
 * Created by alkurop on 2/3/17.
 */
class StreetMarkerView : FrameLayout, IStreetOverlayView {
  val markerView: StreetOverlayView
  val streetView: StreetViewPanoramaView
  val touchOverlay: TouchOverlayView

  var onSteetLoadedSuccess: ((Boolean) -> Unit)? = null
  var onCameraUpdateListener: ((UpdatePosition) -> Unit)? = null

  override var mapsConfig: MapsConfig
    set(value) {
      markerView.mapsConfig = value
    }
    get() = markerView.mapsConfig

  var shouldFocusToMyLocation = true
  var markerDataList = hashSetOf<Place>()

  var cam: StreetViewPanoramaCamera? = null

  @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0)
      : super(context, attrs, defStyleAttr, defStyleRes) {
    inflate(context, R.layout.view_street_marker, this)
    markerView = findViewById(R.id.overlay) as StreetOverlayView
    streetView = findViewById(R.id.panorama) as StreetViewPanoramaView
    touchOverlay = findViewById(R.id.touch) as TouchOverlayView
  }

  override fun onLocationUpdate(location: LatLng) {
    markerView.onLocationUpdate(location)
  }

  override fun onCameraUpdate(cameraPosition: StreetViewPanoramaCamera) {
    markerView.onCameraUpdate(cameraPosition)
  }

  override fun onClick() {
    markerView.onClick()
  }

  override fun setOnMarkerClickListener(onClickListener: ((data: MarkerDrawData) -> Unit)?) {
    markerView.setOnMarkerClickListener(onClickListener)
  }

  private fun sendCameraPosition(position: LatLng) {
    val updatePosition = UpdatePosition(Location(position.latitude,
        position.longitude), 500)
    onCameraUpdateListener?.invoke(updatePosition)
  }

  fun onMarkerClicker(geoData: MarkerGeoData) {
    if (geoData.distance >= mapsConfig.markerMinPositionToMoveToMarker / 1000.toDouble()) {
      focusToLocation(geoData.place.location)
    }
  }

  //CONTROLS

  fun focusToLocation(location: Location) {
    streetView.getStreetViewPanoramaAsync { panorama ->
      panorama.setPosition(LatLng(location.lat, location.lng))
      sendCameraPosition(LatLng(location.lat, location.lng))
    }
  }

  override fun addMarkers(markers: HashSet<Place>) {
    val addMarkers = markers.filter { marker ->
      !markerDataList.contains(marker)
    }
    if (addMarkers.isNotEmpty())
      markerView.addMarkers(markers)
    markerDataList.addAll(addMarkers)
  }


  //State callbacks

  fun onCreate(state: Bundle?) {
    streetView.onCreate(state)
    streetView.getStreetViewPanoramaAsync { panorama ->
      markerView.onCameraUpdate(panorama.panoramaCamera)
      panorama.setOnStreetViewPanoramaCameraChangeListener { cameraPosition ->
        cam = cameraPosition
        markerView.onCameraUpdate(cameraPosition)
      }
      panorama.setOnStreetViewPanoramaChangeListener { cameraPosition ->
        if (cameraPosition !== null && cameraPosition.position !== null) {
          markerView.onLocationUpdate(cameraPosition.position)
          sendCameraPosition(cameraPosition.position)
        }
        onSteetLoadedSuccess?.invoke(cameraPosition !== null && cameraPosition.links != null)
      }
      panorama.setOnStreetViewPanoramaClickListener { onClick() }
    }
    touchOverlay.onTouchListener = {
      markerView.onTouchEvent(it)
    }
    markerView.setOnMarkerClickListener {
      onMarkerClicker(it.matrixData.data)
    }
    restoreState(state)
  }

  private fun restoreState(saveState: Bundle?) {
    saveState?.let {
      shouldFocusToMyLocation = saveState.getBoolean("shouldFocusToMyLocation", true)
      markerDataList = (saveState.getParcelableArray("markerModels") as Array<Place>).toHashSet()
    }
    markerView.addMarkers(markerDataList)
  }

  fun onSaveInstanceState(state: Bundle?): Bundle {
    val bundle = state ?: Bundle()
    bundle.putParcelableArray("markerModels", markerDataList.toTypedArray())
    bundle.putBoolean("shouldFocusToMyLocation", shouldFocusToMyLocation)
    streetView.onSaveInstanceState(bundle)
    markerDataList.clear()
    return bundle
  }

  fun onResume() {
    streetView.onResume()
    cam?.let { markerView.onCameraUpdate(it) }
  }

  fun onPause() {
    streetView.onPause()
  }

  fun onDestroy() {
    streetView.onDestroy()
    markerView.stop()
  }

  fun onLowMemory() = streetView.onLowMemory()
}