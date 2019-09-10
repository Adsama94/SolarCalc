package com.adsama.solarcalculator.interfaces;

import com.adsama.solarcalculator.database.UserLocation;

import java.util.List;

public interface LocationListInterface {
    void getLocationList(List<UserLocation> userLocationList);
}