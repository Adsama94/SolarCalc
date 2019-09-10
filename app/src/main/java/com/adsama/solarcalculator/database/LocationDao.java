package com.adsama.solarcalculator.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * from location_table ORDER BY uId ASC")
    LiveData<List<UserLocation>> getAllUserLocations();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserLocation location);

    @Query("DELETE FROM location_table")
    void deleteAll();

}