package com.adsama.solarcalculator.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.adsama.solarcalculator.database.UserLocation;
import com.adsama.solarcalculator.repository.UserLocationRepository;

import java.util.List;

public class UserLocationViewModel extends AndroidViewModel {

    private UserLocationRepository mRepository;
    private LiveData<List<UserLocation>> mAllLocations;

    public UserLocationViewModel(Application application) {
        super(application);
        mRepository = new UserLocationRepository(application);
        mAllLocations = mRepository.getAllLocations();
    }

    LiveData<List<UserLocation>> getAllLocations() {
        return mAllLocations;
    }

    void insert(UserLocation location) {
        mRepository.insert(location);
    }

}