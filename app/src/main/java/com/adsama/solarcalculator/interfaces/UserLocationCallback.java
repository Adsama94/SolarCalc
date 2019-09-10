package com.adsama.solarcalculator.interfaces;

import com.google.android.gms.common.api.ResolvableApiException;

public interface UserLocationCallback {

    void gpsEnabled();

    void gpsDisabled(ResolvableApiException exception);

}