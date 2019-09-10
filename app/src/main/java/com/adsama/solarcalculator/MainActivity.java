package com.adsama.solarcalculator;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;

import com.adsama.solarcalculator.database.UserLocation;
import com.adsama.solarcalculator.interfaces.UserLocationCallback;
import com.adsama.solarcalculator.repository.UserLocationRepository;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
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

import org.shredzone.commons.suncalc.MoonTimes;
import org.shredzone.commons.suncalc.SunTimes;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static com.adsama.solarcalculator.Utilities.REQUEST_CHECK_SETTINGS;
import static com.adsama.solarcalculator.Utilities.TIME_FORMAT;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnMapReadyCallback, LocationListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 2;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mCallback;
    private LocationRequest mLocationRequest;
    private GoogleMap mGoogleMap;
    private Marker mLocationMarker;
    private LatLng mSelectedLatLng;
    private TextView mCurrentDate, mSunUpTime, mSunDownTime, mMoonUpTime, mMoonDownTime;
    private ImageView mTodayDateView, mPreviousDateView, mNextDateView;
    private long mCurrentTimeInMilliseconds;
    private Calendar mCalendar;
    private MutableLiveData<SunTimes> mSolarMutableLiveData;
    private MutableLiveData<MoonTimes> mLunarMutableLiveData;
    private UserLocationRepository mLocationRepository;
    private LifecycleOwner mLifecycleOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setOnClickListeners();
        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.google_api_key));
        }
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        mLifecycleOwner = this;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(Utilities.UPDATE_TIME);
        mLocationRequest.setFastestInterval(Utilities.FASTEST_TIME);
        mSolarMutableLiveData = new MutableLiveData<>();
        mLunarMutableLiveData = new MutableLiveData<>();
        mLocationRepository = new UserLocationRepository(getApplication());
        MainActivityPermissionsDispatcher.initViewsWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void initViews() {
        mCalendar = Calendar.getInstance();
        mCurrentDate = findViewById(R.id.tv_current_date);
        mSunUpTime = findViewById(R.id.tv_sun_up);
        mSunDownTime = findViewById(R.id.tv_sun_down);
        mMoonUpTime = findViewById(R.id.tv_moon_up);
        mMoonDownTime = findViewById(R.id.tv_moon_down);
        mTodayDateView = findViewById(R.id.iv_day_current);
        mPreviousDateView = findViewById(R.id.iv_day_previous);
        mNextDateView = findViewById(R.id.iv_day_next);
        mCurrentTimeInMilliseconds = Utilities.getCurrentTimeInMillis();
        mCurrentDate.setText(Utilities.getCurrentDate(mCurrentTimeInMilliseconds, mCalendar));
        mCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            }
        };
        checkGpsAndSubscribeToLocation();
    }

    private void setOnClickListeners() {
        mTodayDateView.setOnClickListener(this);
        mPreviousDateView.setOnClickListener(this);
        mNextDateView.setOnClickListener(this);
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

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onLocationDenied() {
        Toast.makeText(this, "Location permission denied!", Toast.LENGTH_SHORT).show();
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForLocation(PermissionRequest request) {
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
            return true;
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
        if (mSelectedLatLng != null) {
            mLocationMarker = Utilities.addUpdateMarker(googleMap, mSelectedLatLng, mLocationMarker);
        }
        updateLocationUI(googleMap);
        checkGpsAndSubscribeToLocation();
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
                        updateSunsetSunriseTime(mSelectedLatLng, mCurrentTimeInMilliseconds);
                        stopLocationUpdates();
                    }
                }
                Toast.makeText(this, "Place: " + place.getName() + ", " + place.getLatLng(), Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void showRationaleDialog(final PermissionRequest request) {
        new AlertDialog.Builder(this).setPositiveButton(getString(R.string.allow), (dialog, which) -> request.proceed()).setNegativeButton(getString(R.string.deny), (dialog, which) -> request.cancel()).setCancelable(false).setMessage(R.string.permission_location_rationale).show();
    }

    private void saveLocation() {
        if (mSelectedLatLng != null) {
            UserLocation userLocation = new UserLocation();
            userLocation.latitude = mSelectedLatLng.latitude;
            userLocation.longitude = mSelectedLatLng.longitude;
            mLocationRepository.insert(userLocation);
            Toast.makeText(this, "Location saved successfully", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "saved data is " + mSelectedLatLng.latitude + " " + mSelectedLatLng.longitude);
        } else {
            Toast.makeText(this, "NO LOCATION SELECTED!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavedLocationsDialog() {
        Utilities.savedLocationDialogBox(mLifecycleOwner, this, mLocationRepository.getAllLocations(), latLng -> {
            mSelectedLatLng = latLng;
            mLocationMarker = Utilities.addUpdateMarker(mGoogleMap, mSelectedLatLng, mLocationMarker);
            updateSunsetSunriseTime(mSelectedLatLng, mCurrentTimeInMilliseconds);
        });
    }

    private void checkGpsAndSubscribeToLocation() {
        if (Utilities.checkGpsStatus(this)) {
            startLocationUpdates();
        } else {
            Utilities.requestGpsSettings(mLocationRequest, this, new UserLocationCallback() {
                @Override
                public void gpsEnabled() {
                    startLocationUpdates();
                }

                @Override
                public void gpsDisabled(ResolvableApiException exception) {
                    try {
                        exception.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void startLocationUpdates() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mCallback, Looper.myLooper());
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mCallback);
    }

    private void setDateToTextView(long currentTimeInMilliseconds) {
        mCurrentTimeInMilliseconds = currentTimeInMilliseconds;
        mCurrentDate.setText(Utilities.getCurrentDate(currentTimeInMilliseconds, mCalendar));
        if (mSelectedLatLng != null) {
            updateSunsetSunriseTime(mSelectedLatLng, currentTimeInMilliseconds);
        }
    }

    private void setDate(String choice) {
        switch (choice) {
            case Utilities.CURRENT_DAY:
                setDateToTextView(Utilities.getCurrentTimeInMillis());
                break;
            case Utilities.NEXT_DAY:
                setDateToTextView(Utilities.getNextDay(mCurrentTimeInMilliseconds, mCalendar));
                break;
            case Utilities.PREVIOUS_DAY:
                setDateToTextView(Utilities.getPreviousDay(mCurrentTimeInMilliseconds, mCalendar));
                break;
        }
    }

    private void updateSunsetSunriseTime(LatLng latLng, long millis) {
        Date date = new Date(millis);
        SunTimes sunTimes = SunTimes.compute().on(date).at(latLng.latitude, latLng.longitude).execute();
        MoonTimes moonTimes = MoonTimes.compute().on(date).at(latLng.latitude, latLng.longitude).execute();
        mSolarMutableLiveData.setValue(sunTimes);
        mLunarMutableLiveData.setValue(moonTimes);
        MutableLiveData<SunTimes> goldenHourSunTime = new MutableLiveData<>();
        SunTimes goldenHourTime = SunTimes.compute().twilight(SunTimes.Twilight.GOLDEN_HOUR).on(date).at(latLng.latitude, latLng.longitude).execute();
        goldenHourSunTime.setValue(goldenHourTime);
        goldenHourSunTime.observe(this, sunTimes1 -> {
            if (sunTimes1 != null && sunTimes1.getRise() != null) {
                Utilities.scheduleGoldenHourTask(this, sunTimes.getRise().getTime());
            }
            if (sunTimes1 != null && sunTimes1.getSet() != null) {
                Utilities.scheduleGoldenHourTask(this, sunTimes.getRise().getTime());
            }
        });
        mSolarMutableLiveData.observe(this, sunTimes12 -> {
            if (sunTimes12 != null) {
                if (sunTimes12.getRise() != null) {
                    String sunRiseTime = Utilities.getFormattedDateForGolderHours(sunTimes12.getRise(), TIME_FORMAT, mCalendar);
                    mSunUpTime.setText(sunRiseTime);
                }
                if (sunTimes12.getSet() != null) {
                    String sunSetTime = Utilities.getFormattedDateForGolderHours(sunTimes12.getSet(), TIME_FORMAT, mCalendar);
                    mSunDownTime.setText(sunSetTime);
                }
            }
        });
        mLunarMutableLiveData.observe(this, moonTimes1 -> {
            if (moonTimes1 != null) {
                if (moonTimes1.getRise() != null) {
                    String moonRiseTime = Utilities.getFormattedDateForGolderHours(moonTimes1.getRise(), TIME_FORMAT, mCalendar);
                    mMoonUpTime.setText(moonRiseTime);
                }
                if (moonTimes1.getSet() != null) {
                    String moonSetTime = Utilities.getFormattedDateForGolderHours(moonTimes1.getSet(), TIME_FORMAT, mCalendar);
                    mMoonDownTime.setText(moonSetTime);
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng changedLatLong = new LatLng(location.getLatitude(), location.getLongitude());
        mLocationMarker = Utilities.addUpdateMarker(mGoogleMap, changedLatLong, mLocationMarker);
        mSelectedLatLng = changedLatLong;
        updateSunsetSunriseTime(mSelectedLatLng, mCurrentTimeInMilliseconds);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.iv_day_current:
                setDate(Utilities.CURRENT_DAY);
                break;
            case R.id.iv_day_next:
                setDate(Utilities.NEXT_DAY);
                break;
            case R.id.iv_day_previous:
                setDate(Utilities.PREVIOUS_DAY);
                break;
        }
    }

}