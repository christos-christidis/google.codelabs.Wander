package com.advanced.wander;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.StreetViewPanoramaOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;

import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final int GRANTED = PackageManager.PERMISSION_GRANTED;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, mapFragment)
                .commit();

        mapFragment.getMapAsync(this);
    }


    // If Google Play services is not installed on the device, the user will be prompted to install
    // it inside the SupportMapFragment. This method will only be triggered once the user has
    // installed Google Play services and returned to the app.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng home = new LatLng(38.052296, 23.792727);

        // SOS: second arg is zoom level (1-20)
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(home, 15);
        mMap.moveCamera(cameraUpdate);

        setOnMapLongClickListener();

        setOnPoiClickListener();

        setOnInfoWindowClickListener();

        addGroundOverlay(home);

        setMapStyle();

        enableMyLocation();
    }

    private void setOnMapLongClickListener() {
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                String snippet = String.format(Locale.getDefault(),
                        "Lat: %1$.5f, Long: %2$.5f", latLng.latitude, latLng.longitude);

                mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.dropped_pin))
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            }
        });
    }

    private void setOnPoiClickListener() {
        mMap.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(PointOfInterest poi) {
                Marker poiMarker = mMap.addMarker(new MarkerOptions()
                        .position(poi.latLng)
                        .title(poi.name));
                // SOS: used to differentiate between markers. Only these POI markers will open
                // StreetView when their info window is clicked
                poiMarker.setTag("poi");
                poiMarker.showInfoWindow();
            }
        });
    }

    private void setOnInfoWindowClickListener() {
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if (marker.getTag() == "poi") {
                    StreetViewPanoramaOptions options = new StreetViewPanoramaOptions()
                            .position(marker.getPosition());

                    SupportStreetViewPanoramaFragment streetViewFragment =
                            SupportStreetViewPanoramaFragment.newInstance(options);

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, streetViewFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }

    private void addGroundOverlay(LatLng home) {
        // SOS: add image w size = 100m @ home
        GroundOverlayOptions homeOverlay = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.android))
                .position(home, 100);

        mMap.addGroundOverlay(homeOverlay);
    }

    private void setMapStyle() {
        try {
            // SOS: The JSON in the file was created w the help of https://mapstyle.withgoogle.com
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style));

            if (!success) {
                Log.e(LOG_TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(LOG_TAG, "Can't find style. Error: ", e);
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION) == GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{LOCATION_PERMISSION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mMap != null) {
            switch (item.getItemId()) {
                case R.id.normal_map:
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    return true;
                case R.id.hybrid_map:
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    return true;
                case R.id.satellite_map:
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    return true;
                case R.id.terrain_map:
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == GRANTED) {
                enableMyLocation();
            }
        }
    }
}
