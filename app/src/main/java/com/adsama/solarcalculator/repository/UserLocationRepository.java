package com.adsama.solarcalculator.repository;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.adsama.solarcalculator.database.LocationDao;
import com.adsama.solarcalculator.database.LocationDb;
import com.adsama.solarcalculator.database.UserLocation;

import java.util.List;

public class UserLocationRepository {

    private LocationDao mLocationDao;
    private LiveData<List<UserLocation>> mAllUserLocations;

    public UserLocationRepository(Application application) {
        LocationDb db = LocationDb.getDatabase(application);
        mLocationDao = db.locationDao();
        mAllUserLocations = mLocationDao.getAllUserLocations();
    }

    public LiveData<List<UserLocation>> getAllLocations() {
        return mAllUserLocations;
    }


    public void insert(UserLocation location) {
        new insertAsyncTask(mLocationDao).execute(location);
    }

    private static class insertAsyncTask extends AsyncTask<UserLocation, Void, Void> {

        private LocationDao mAsyncTaskDao;

        insertAsyncTask(LocationDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final UserLocation... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }


}