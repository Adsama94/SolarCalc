package com.adsama.solarcalculator.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * FROM location_table")
    List<UserLocation> getAllUserLocations();

    @Query("SELECT * FROM location_table WHERE uid IN (:locationIds)")
    List<UserLocation> loadLocationById(int[] locationIds);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Long[] insertUserLocation(UserLocation... locations);

    @Delete
    void deleteUserLocaiton(UserLocation userLocation);

}