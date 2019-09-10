package com.adsama.solarcalculator.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.adsama.solarcalculator.R;
import com.adsama.solarcalculator.receiver.GoldenReceiver;

public class GoldenHourWorker extends Worker {

    public GoldenHourWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        getApplicationContext().sendBroadcast(new Intent(getApplicationContext(), GoldenReceiver.class).setAction(getApplicationContext().getString(R.string.golden_hour_action)));
        return Result.success();
    }

}