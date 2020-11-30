package com.example.group3;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DrawerLayout drawer;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    TextView showEmail, showUsername;
    String email, username;
    Button camBtn;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Marker marker;


    public ArrayList<LatLng> markersList;
    //LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        email = SaveSharedPreference.getEmail(MapsActivity.this);
        username = SaveSharedPreference.getUserName(MapsActivity.this);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.map_view);

        NavigationView navigationView = findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        showEmail = headerView.findViewById(R.id.showEmail);
        showUsername = headerView.findViewById(R.id.showUsername);
        showEmail.setText(email);
        showUsername.setText(username);

        /*
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
         */

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(this);

        //Initialize fuse location
        client = LocationServices.getFusedLocationProviderClient(this);

        //Check permission
        if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            //If permission denied request again
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
        //Get and build markers Arraylist!
        getMarkers();

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.side_info:
                        startActivity(new Intent(getApplicationContext(), InfoActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.side_logout:
                        SaveSharedPreference.clearUser(MapsActivity.this);
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu:

                        if (!drawer.isDrawerOpen(GravityCompat.START))
                            drawer.openDrawer(GravityCompat.START);
                        else drawer.closeDrawer(GravityCompat.END);
                        return true;

                    case R.id.map_view:
                        return true;

                    case R.id.camera:
                        startActivity(new Intent(getApplicationContext(), cameraActivity.class));
                        finish(); //Tämä sulkee maps activityn joten ku kamera/CreateStory activitysta tullaan niin mennäänki suoraan mainActivityyn.
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.explore:
                        startActivity(new Intent(getApplicationContext(), ExploreActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.profile:
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;
                }
                return false;
            }
        });

    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ///TÄMÄ ON LAST LOCATION EI CURRENT
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    supportMapFragment.getMapAsync((new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            LatLng latLng = new LatLng(location.getLatitude()
                                    , location.getLongitude());

                            //Add marker
                            MarkerOptions options = new MarkerOptions().position(latLng)
                                    .title("You are here!");
                            //Zoom map
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                            //Add marker on map
                            googleMap.addMarker(options);

                        }
                    }));
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //When permission granted
                //Call method
                getCurrentLocation();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        getMarkers();
        getCurrentLocation();

        mMap = googleMap;
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        for (int i = 0; i < markersList.size(); i++) {
            marker = mMap.addMarker(
                    new MarkerOptions().
                            position(markersList.get(i)).
                            icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pika)).
                            title("Marker" + i));

            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(markersList.get(i)));
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
        }
    };


    public ArrayList<LatLng> getMarkers(){
        //Loops and adds new markers to the list
        markersList = new ArrayList<LatLng>();

        LatLng oulu1 = new LatLng(65.0121, 25.4651);
        LatLng hukassa = new LatLng(5.0000, 15.0000);
        LatLng oulu3 = new LatLng(15.1241, 25.2121);
        LatLng oulu4 = new LatLng(64.0021, 37.1201);
        LatLng oulu5 = new LatLng(65.0822, 1.1002);

        markersList.add(oulu1);
        markersList.add(hukassa);
        markersList.add(oulu3);
        markersList.add(oulu4);
        markersList.add(oulu5);

        return markersList;

    }

    private class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        String imageUri = "https://i.imgur.com/tGbaZCY.jpg";
        String storyDesc = "Tässä on storyn description";

        private View view;

        public CustomInfoWindowAdapter() {
            view = getLayoutInflater().inflate(R.layout.popup,null);
        }

        @Override
        public View getInfoContents(Marker marker) {

            if (MapsActivity.this.marker != null
                    && MapsActivity.this.marker.isInfoWindowShown()) {
                MapsActivity.this.marker.hideInfoWindow();
                MapsActivity.this.marker.showInfoWindow();
            }
            return null;
        }

        @Override
        public View getInfoWindow(final Marker marker) {
            MapsActivity.this.marker = marker;

            ImageView image = view.findViewById(R.id.image);
            TextView desc = view.findViewById(R.id.textView);

            desc.setText(storyDesc);

            Picasso.get()
                    .load(imageUri)
                    .error(R.mipmap.ic_launcher) // will be displayed if the image cannot be loaded
                    .into(image);

            //getInfoContents(marker);
            return view;
        }
    }

}
