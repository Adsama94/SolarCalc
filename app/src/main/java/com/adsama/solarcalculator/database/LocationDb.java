package com.adsama.solarcalculator.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UserLocation.class}, version = 1)
public abstract class LocationDb extends RoomDatabase {

    private static volatile LocationDb INSTANCE;

    public static LocationDb getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (LocationDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context, LocationDb.class, "location_database").build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract LocationDao locationDao();

}