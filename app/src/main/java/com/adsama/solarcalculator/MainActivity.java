package com.adsama.solarcalculator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.adsama.solarcalculator.database.UserLocation;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.Arrays;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mGoogleMap;
    private Marker mLocationMarker;
    private LatLng mSelectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Places.initialize(this, getString(R.string.google_api_key));
        FloatingActionButton currentLocationButton = findViewById(R.id.fab_current_location);
        currentLocationButton.setOnClickListener(view -> MainActivityPermissionsDispatcher.getCurrentLocationWithPermissionCheck(MainActivity.this));
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void updateLocationUI(GoogleMap map) {
        if (map == null) {
            return;
        }
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Error updating location UI " + e.getMessage());
        }
        getDeviceLocation(map);
    }

    private void getDeviceLocation(GoogleMap map) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Task locationResult = mFusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        Location currentLocation = (Location) task.getResult();
                        if (currentLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 15));
                            Log.i(LOG_TAG, "CURRENT LATITUDE IS " + currentLocation.getLatitude());
                            Log.i(LOG_TAG, "CURRENT LONGITUDE IS " + currentLocation.getLongitude());
                        }
                    } else {
                        Log.d(LOG_TAG, "Current location is null. Using defaults.");
                        Log.e(LOG_TAG, "Exception: %s", task.getException());
                        map.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void getCurrentLocation() {
        Toast.makeText(this, "PERMISSION GRANTED. GETTING CURRENT LOCATION", Toast.LENGTH_SHORT).show();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onLocationDenied() {
        // NOTE: Deal with a denied permission, e.g. by showing specific UI
        // or disabling certain functionality
        Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show();
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForLocation(PermissionRequest request) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        showRationaleDialog(request);
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onLocationNeverAskAgain() {
        Toast.makeText(this, "Location permission will never be asked again!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_location_search);
        MenuItem saveLocationItem = menu.findItem(R.id.action_save_location);
        saveLocationItem.setOnMenuItemClickListener(menuItem -> false);
        MenuItem showSavedLocationItem = menu.findItem(R.id.action_show_saved_location);
        showSavedLocationItem.setOnMenuItemClickListener(menuItem -> false);
        searchItem.setOnMenuItemClickListener(menuItem -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(MainActivity.this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
            return false;
        });
        saveLocationItem.setOnMenuItemClickListener(menuItem -> {
            saveLocation();
            return false;
        });
        showSavedLocationItem.setOnMenuItemClickListener(menuItem -> {
            showSavedLocationsDialog();
            return false;
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        updateLocationUI(googleMap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                if (mGoogleMap != null) {
                    mSelectedLatLng = place.getLatLng();
                    if (mSelectedLatLng != null) {
                        mLocationMarker = Utilities.addUpdateMarker(mGoogleMap, mSelectedLatLng, mLocationMarker);
//                        changeRiseSetTime(mSelectedLatLng, currentTimeInMilliseconds);
//                        stopLocationUpdates();
                    }
                }
                Log.i(LOG_TAG, "Place: " + place.getName() + ", " + place.getLatLng());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null)
                    Log.i(LOG_TAG, status.getStatusMessage());
                Toast.makeText(this, "Error fetching places " + status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void showRationaleDialog(final PermissionRequest request) {
        new AlertDialog.Builder(this).setPositiveButton(getString(R.string.allow), (dialog, which) -> request.proceed()).setNegativeButton(getString(R.string.deny), (dialog, which) -> request.cancel()).setCancelable(false).setMessage(R.string.permission_location_rationale).show();
    }

    private void saveLocation() {
        if (mSelectedLatLng != null) {
            UserLocation userLocation = new UserLocation();
            userLocation.latitude = mSelectedLatLng.latitude;
            userLocation.longitude = mSelectedLatLng.longitude;
//            pinsRepo.insertPin(pins, this);
        } else {
            Toast.makeText(this, "NO LOCATION SELECTED!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavedLocationsDialog() {
        Utilities.savedLocationDialogBox();
    }

    private void checkGpsAndSubscribeToLocation() {
//        if (Utilities.checkGpsStatus(this)) {
//            startLocationUpdates();
//        } else {
//            Utilities.requestGpsSettings(locationRequest, this, new LocationSettingsCallback() {
//                @Override
//                public void gpsTurnedOn() {
//                    startLocationUpdates();
//                }
//
//                @Override
//                public void gpsTurnedOff(ResolvableApiException resolvable) {
//                    try {
//                        resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
//                    } catch (IntentSender.SendIntentException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng changedLatLong = new LatLng(location.getLatitude(), location.getLongitude());
        mLocationMarker = Utilities.addUpdateMarker(mGoogleMap, changedLatLong, mLocationMarker);
        mSelectedLatLng = changedLatLong;
    }

}