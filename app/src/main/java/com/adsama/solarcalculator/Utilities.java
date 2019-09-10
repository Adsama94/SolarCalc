package com.adsama.solarcalculator;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.adsama.solarcalculator.adapter.LocationListAdapter;
import com.adsama.solarcalculator.database.UserLocation;
import com.adsama.solarcalculator.interfaces.AdapterLocationInterface;
import com.adsama.solarcalculator.interfaces.UserLocationCallback;
import com.adsama.solarcalculator.service.GoldenHourWorker;
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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

class Utilities {

    static final int REQUEST_CHECK_SETTINGS = 2525;
    static final String TIME_FORMAT = "hh:MM a";
    static final String PREVIOUS_DAY = "previous";
    static final String CURRENT_DAY = "current";
    static final String NEXT_DAY = "next";
    static final long UPDATE_TIME = 180000;//three minutes
    static final long FASTEST_TIME = 60000;//one minute

    static long getCurrentTimeInMillis() {
        return System.currentTimeMillis();
    }

    static Marker addUpdateMarker(GoogleMap googleMap, LatLng latLng, Marker marker) {
        if (marker == null) {
            marker = googleMap.addMarker(new MarkerOptions().position(latLng));
        } else {
            marker.setPosition(latLng);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        return marker;
    }

    static void savedLocationDialogBox(LifecycleOwner lifecycleOwner, Activity context, LiveData<List<UserLocation>> locationArrayList, AdapterLocationInterface positionInterface) {
        List<UserLocation>[] locationList = new List[]{new ArrayList<>()};
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.location_dialog_box, null);
        dialogBuilder.setCancelable(true);
        dialogBuilder.setView(dialogView);
        AlertDialog alertDialog = dialogBuilder.create();
        Button closeButton = dialogView.findViewById(R.id.btn_close_dialog);
        TextView emptyLocations = dialogView.findViewById(R.id.tv_empty_dialog);
        RecyclerView locationRecyclerView = dialogView.findViewById(R.id.rv_dialog_box);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        LocationListAdapter locationAdapter = new LocationListAdapter(context, positionInterface, locationList[0], alertDialog);
        locationRecyclerView.setLayoutManager(linearLayoutManager);
        locationRecyclerView.setAdapter(locationAdapter);
        locationArrayList.observe(lifecycleOwner, userLocations -> {
            if (userLocations != null && userLocations.size() > 0) {
                locationRecyclerView.setVisibility(View.VISIBLE);
                emptyLocations.setVisibility(View.GONE);
                ArrayList<UserLocation> actualList = new ArrayList<>(userLocations);
                locationAdapter.updateList(actualList);
            } else {
                locationRecyclerView.setVisibility(View.GONE);
                emptyLocations.setVisibility(View.VISIBLE);
            }
        });
        closeButton.setOnClickListener(v -> alertDialog.dismiss());
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.rounded_button_background));
        }
        alertDialog.show();
    }

    static void scheduleGoldenHourTask(Context context, long timeMillis) {
        OneTimeWorkRequest hourRequest;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            hourRequest = new OneTimeWorkRequest.Builder(GoldenHourWorker.class).setInitialDelay(Duration.ofMillis(timeMillis)).build();
        } else {
            hourRequest = new OneTimeWorkRequest.Builder(GoldenHourWorker.class).setInitialDelay(timeMillis, TimeUnit.MILLISECONDS).build();
        }
        WorkManager.getInstance(context).enqueue(hourRequest);
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

    static String getCurrentDate(long millis, Calendar calendar) {
        return getFormattedDate(millis, "EEEE, MMMM dd, yyyy", calendar);
    }

    static String getFormattedDate(long milliSeconds, String dateFormat, Calendar calendar) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    static String getFormattedDateForGolderHours(Date date, String dateFormat, Calendar calendar) {
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        calendar.setTime(date);
        return formatter.format(calendar.getTime());
    }

    static long getPreviousDay(long millis, Calendar calendar) {
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTimeInMillis();
    }

    static long getNextDay(long millis, Calendar calendar) {
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTimeInMillis();
    }

}