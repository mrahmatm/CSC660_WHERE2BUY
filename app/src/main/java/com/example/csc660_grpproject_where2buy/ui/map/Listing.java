package com.example.csc660_grpproject_where2buy.ui.map;

import com.google.android.gms.maps.model.LatLng;

public class Listing {
    private String previewImage;
    private String storeName;
    private String date;
    private LatLng storeLocation;

    public Listing(String previewImage, String storeName, String date, LatLng storeLocation) {
        this.previewImage = previewImage;
        this.storeName = storeName;
        this.date = date;
        this.storeLocation = storeLocation;
    }

    public String getPreviewImage() {
        return previewImage;
    }

    public void setPreviewImage(String previewImage) {
        this.previewImage = previewImage;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public LatLng getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(LatLng storeLocation) {
        this.storeLocation = storeLocation;
    }
}
