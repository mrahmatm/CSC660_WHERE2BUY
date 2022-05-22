package com.example.csc660_grpproject_where2buy.ui.respond;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.csc660_grpproject_where2buy.MainActivity;
import com.example.csc660_grpproject_where2buy.R;
import com.example.csc660_grpproject_where2buy.RequestsNearby;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ListViewRespond extends ArrayAdapter<RequestsNearby> {
    private final Context context;
    ArrayList<RequestsNearby> requests;
    private String responderID;

    public ListViewRespond(Context context, ArrayList<RequestsNearby> requests, String responderID){
        super(context, R.layout.layout_listitems, requests);
        this.context = context;
        this.requests = requests;
        this.responderID = responderID;
    }

    public View getView(int position, View view, ViewGroup parent){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.layout_listitems, null, true);

        TextView message = (TextView) rowView.findViewById(R.id.respondText);
        ImageButton imageButton = (ImageButton) rowView.findViewById(R.id.respondButton);

        RequestsNearby item = requests.get(position);

        message.setText(item.toString());
        message.setClickable(true);

        message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customOnClick(position);
            }
        });
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                customOnClick(position);
                // go respond or something
            }
        });

        return rowView;
    }

    private void customOnClick(int position){
        RequestsNearby item = requests.get(position);
        //Toast.makeText(context, "id " + item.getRequestID() + "\n" + item, Toast.LENGTH_SHORT).show();
        String msg = "";
        Intent i = new Intent(context, RespondActivity.class);
        i.putExtra("requestID", item.getRequestID());
        i.putExtra("responderID", responderID);

        context.startActivity(i);
    }
}
