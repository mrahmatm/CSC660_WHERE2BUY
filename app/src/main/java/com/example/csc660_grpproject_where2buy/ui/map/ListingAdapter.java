package com.example.csc660_grpproject_where2buy.ui.map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.ui.respond.RespondActivity;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class ListingAdapter extends ArrayAdapter<Listing> {
    private  static  final String TAG = "ListingAdapter";
    private Context mContext;
    int mResource;

    public ListingAdapter(@NonNull Context context, int resource, ArrayList<Listing> objects) {
        super(context, resource, objects);
        this.mContext = context ;
        this.mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position,  View convertView,  ViewGroup parent) {
        String storeName = getItem(position).getStoreName();
        String image64 = getItem(position).getPreviewImage();
        String date = getItem(position).getDate();
        LatLng location = getItem(position).getStoreLocation();

        Listing respond = new Listing(image64, storeName, date, location);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        ImageView displayImage = (ImageView) convertView.findViewById(R.id.previewImage);
        TextView txtStoreName = (TextView) convertView.findViewById(R.id.txtStoreName);
        TextView txtRespondDate = (TextView) convertView.findViewById(R.id.txtRespondDate);

        displayImage.setImageBitmap(StringToBitMap(image64));
        txtStoreName.setText(storeName);
        txtRespondDate.setText(date);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG+"?", "onClick: CLICKED!"+date);
                Intent intent = new Intent(mContext, ViewRespondLocation.class);
                //int tempResponder = Integer.parseInt(userID);
                intent.putExtra("position", location.toString());
                //intent.putExtra("responderID", tempResponder);
                mContext.startActivity(intent);
            }
        });

        return convertView;
    }

    public Bitmap StringToBitMap(String encodedString){
        try{
            byte [] encodeByte = Base64.decode(encodedString,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        }
        catch(Exception e){
            e.getMessage();
            return null;
        }
    }
}
