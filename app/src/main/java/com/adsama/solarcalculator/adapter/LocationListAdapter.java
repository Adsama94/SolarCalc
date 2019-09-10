package com.adsama.solarcalculator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.adsama.solarcalculator.R;
import com.adsama.solarcalculator.database.UserLocation;
import com.adsama.solarcalculator.interfaces.AdapterLocationInterface;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.LocationHolder> {

    private final Context mContext;
    private final AdapterLocationInterface mLocationInterface;
    private List<UserLocation> mLocationList;
    private AlertDialog mAlertDialog;

    public LocationListAdapter(Context context, AdapterLocationInterface locationInterface, List<UserLocation> locationArrayList, AlertDialog alertDialog) {
        mContext = context;
        mLocationInterface = locationInterface;
        mLocationList = locationArrayList;
        mAlertDialog = alertDialog;
    }

    @NonNull
    @Override
    public LocationHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.location_list_item, parent, false);
        return new LocationHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationHolder holder, int position) {
        UserLocation location = mLocationList.get(position);
        holder.mLocationTextView.setText(location.latitude + "" + location.longitude);
        holder.itemView.setOnClickListener(view -> {
            LatLng latLng = new LatLng(location.latitude, location.longitude);
            mLocationInterface.getSelectedLocation(latLng);
            mAlertDialog.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        if (mLocationList != null) {
            return mLocationList.size();
        }
        return 0;
    }

    public void updateList(ArrayList<UserLocation> userLocations) {
        mLocationList = userLocations;
        notifyDataSetChanged();
    }

    class LocationHolder extends RecyclerView.ViewHolder {

        final TextView mLocationTextView;

        LocationHolder(@NonNull View itemView) {
            super(itemView);
            mLocationTextView = itemView.findViewById(R.id.location_text);
        }
    }

}