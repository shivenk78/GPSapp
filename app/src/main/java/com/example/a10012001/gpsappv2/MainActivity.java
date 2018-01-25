package com.example.a10012001.gpsappv2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.internal.IGoogleMapDelegate;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    ArrayList<Marker> markers;
    BufferedReader reader;
    float distance;
    GoogleMap aMap;
    List<Address> addressList;
    Location startLoc;
    LocationManager locMan;
    TextView address, distanceText, lat, lon;
    String formattedAddress, unit;
    static String latty, longy;
    Spinner units;
    SupportMapFragment mapFragment;
    JSONArray results;
    final int PERM_REQ = 1203984;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distance = 0;
        latty = longy = "";
        lat = (TextView) findViewById(R.id.id_lat);
        lon = (TextView) findViewById(R.id.id_long);
        address = (TextView) findViewById(R.id.id_address);
        distanceText = (TextView) findViewById(R.id.id_distance);
        units = (Spinner) findViewById(R.id.id_units);

        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        Location lastLoc = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        startLoc = lastLoc;
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map); //Map, marker, and zoom from StackOverflow
        mapFragment.getMapAsync(this);

        String[] unitTypes = {"MILES", "FEET", "KM", "M"};
        ArrayAdapter<String> spinAdapt = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, unitTypes);
        units.setAdapter(spinAdapt);
        units.setSelection(3);
        units.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    unit = "miles";
                if (i == 1)
                    unit = "feet";
                if (i == 2)
                    unit = "km";
                if (i == 3)
                    unit = "m";
                distanceText.setText("Distance Traveled: " + getDistance());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("shiven", "hey look it worked");

        longy = "" + location.getLongitude();
        latty = "" + location.getLatitude();
        Log.d("shiven", latty + ",   " + longy);

        distance += startLoc.distanceTo(location);
        startLoc = location;

        AsyncThread async = new AsyncThread();
        async.execute();

        LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude()); //Map, marker, and zoom from StackOverflow
        aMap.addMarker(new MarkerOptions().position(currentLoc).title("Current Location"));
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc,10));

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        aMap = googleMap;
        onLocationChanged(startLoc);
    }

    public class AsyncThread extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                URL website = new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng="+latty+","+longy+"&key=AIzaSyCaacL3im9J_UYDepC15QRNU85oGLWATrk");
                Log.d("shiven","URL created");
                URLConnection connection = website.openConnection();
                Log.d("shiven","URL connection created");
                InputStream inputStream = connection.getInputStream();
                Log.d("shiven","inputStream created");
                reader = new BufferedReader(new InputStreamReader(inputStream));
            } catch (Exception e) {
                Log.d("shiven","TryCatch caught an Exception "+e.toString());
            }
            try {
                String nextLn;
                String initialJson="";
                while ((nextLn = reader.readLine()) != null) {
                    initialJson += nextLn;
                    Log.d("shiven", "line reading");
                }
                Log.d("shiven",initialJson);
                JSONObject json = new JSONObject(initialJson);
                results = json.getJSONArray("results");
                json = results.getJSONObject(0);
                formattedAddress=json.getString("formatted_address");
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("shiven", "TryCatch caught an Exception " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            latty+="00000";
            longy+="00000";
            lat.setText("Latitude: "+latty.substring(0,latty.indexOf('.')+5));
            lon.setText("Longitude: "+longy.substring(0,longy.indexOf('.')+5));
            address.setText(formattedAddress);
            distanceText.setText("Distance Traveled: "+getDistance());
        }
    }

    public String getDistance(){
        String temp = distance+"";
        if(unit.equals("miles")){
            temp = distance/1609.34+"";
            return temp.substring(0,temp.indexOf('.')+2)+" miles";
        }
        if(unit.equals("feet")){
            temp=""+(distance*3.2808);
            return temp.substring(0,temp.indexOf('.')+2)+" feet";
        }
        if(unit.equals("km")){
            temp=distance/1000+"";
            return temp.substring(0,temp.indexOf('.')+2)+" km";
        }
        temp=distance+"";
        return temp.substring(0,temp.indexOf('.')+2)+" m";
    }
}
