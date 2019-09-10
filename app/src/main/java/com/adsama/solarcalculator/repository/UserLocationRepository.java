package com.adsama.solarcalculator.repository;

import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import com.adsama.solarcalculator.database.LocationDao;
import com.adsama.solarcalculator.database.LocationDb;
import com.adsama.solarcalculator.database.UserLocation;

import java.util.List;

public class UserLocationRepository {

    private LocationDao mLocationDao;
    private MutableLiveData<List<UserLocation>> mUserLocationList;

    public UserLocationRepository(Application application) {
        LocationDb db = LocationDb.getDatabase(application);
        mLocationDao = db.locationDao();
        mUserLocationList = new MutableLiveData<>();
    }

}