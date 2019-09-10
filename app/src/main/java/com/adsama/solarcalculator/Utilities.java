package com.adsama.solarcalculator;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;

import com.adsama.solarcalculator.interfaces.UserLocationCallback;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

class Utilities {

    static Marker addUpdateMarker(GoogleMap googleMap, LatLng latLng, Marker marker) {
        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(latLng));
        } else {
            marker.setPosition(latLng);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        return marker;
    }

    static void savedLocationDialogBox() {

    }

    static boolean checkGpsStatus(Activity activity) {
        LocationManager manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (manager != null) {
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
        return false;
    }

    static void requestGpsSettings(LocationRequest locationRequest, Activity activity, UserLocationCallback userLocationCallback) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(activity);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        builder.setAlwaysShow(true);
        task.addOnSuccessListener(activity, locationSettingsResponse -> userLocationCallback.gpsEnabled());
        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                ResolvableApiException resolvable = (ResolvableApiException) e;
                userLocationCallback.gpsDisabled(resolvable);
            }
        });
    }

}