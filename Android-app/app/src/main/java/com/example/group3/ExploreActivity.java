package com.example.group3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.time.OffsetDateTime;
import java.util.regex.Pattern;


public class ExploreActivity extends AppCompatActivity {

    private DrawerLayout drawer;
    TextView showEmail, showUsername;
    String email, username, description, image, postersUsername, lat, lng, isoTime, search, storyId;
    String niceDateStr;
    RequestQueue requestQueue;
    JSONArray stories;
    JSONObject story;
    ListView listView;
    EditText searchBar;
    private int loadCount = 0;

    ArrayList<String> storyIdList = new ArrayList<String>();

    ArrayList<ListViewItem> listViewItems = new ArrayList<>();

    SwipeRefreshLayout swipeView;
    boolean flag_loading = false;
    boolean allStoriesLoaded = false;
    boolean onSearch = false;
    int firstItemLoaded = 0;
    CustomListView adapter;
    int currentFirstVisibleItem, currentVisibleItemCount, currentTotalItemCount, currentScrollState;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explore);


        requestQueue = Volley.newRequestQueue(this);
        listView = findViewById(R.id.storyListView);
        searchBar = findViewById(R.id.searchBar);
        swipeView = findViewById(R.id.swiperefresh);


        getStories("http://100.26.132.75/story?number=10");

        swipeView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                /*maintitle.clear();
                subtitle.clear();
                imgid.clear();
                usernameArraylist.clear();
                latitude.clear();
                longitude.clear();
                timestamp.clear();
                getStories("http://100.26.132.75/story");*/
                finish();
                startActivity(getIntent());
                overridePendingTransition(0, 0);
                swipeView.setRefreshing(false);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                /*String lat = latitude.get(i);
                String lng = longitude.get(i);
                Intent mapsIntent = new Intent(ExploreActivity.this, MapsActivity.class);
                mapsIntent.putExtra("latitude", lat);
                mapsIntent.putExtra("longitude", lng);*/
                //startActivity(mapsIntent);
                Intent viewStoryIntent = new Intent(getApplicationContext(), ViewStoryActivity.class);
                viewStoryIntent.putExtra("storyid", storyIdList.get(i));
                startActivity(viewStoryIntent);

                //Toast.makeText(ExploreActivity.this, ""+ timestamp.get(i), Toast.LENGTH_SHORT).show();
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                currentFirstVisibleItem = firstVisibleItem;
                currentVisibleItemCount = visibleItemCount;
                currentTotalItemCount = totalItemCount;
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
                currentScrollState = scrollState;
                this.isScrollCompleted();
            }

            private void isScrollCompleted() {

                if (currentFirstVisibleItem + currentVisibleItemCount >= currentTotalItemCount - 3) {
                    if (currentVisibleItemCount > 0
                            && currentScrollState == SCROLL_STATE_IDLE) {

                        if(currentTotalItemCount != 0) {
                            if(!flag_loading && !onSearch)
                            {
                                flag_loading = true;
                                loadCount++;
                                addItems(loadCount);
                            }
                        }
                    }
                }
            }
        });

        searchBar.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                final int DRAWABLE_RIGHT = 2;

                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (motionEvent.getRawX() >= (searchBar.getRight() - searchBar.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        onSearch = true;
                        firstItemLoaded = 0;
                        listViewItems.clear();
                        getStories("http://100.26.132.75/story/search/" + searchBar.getText().toString());
                    }

                }

                return false;
            }

        });
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH) {
                    onSearch = true;
                    firstItemLoaded = 0;
                    listViewItems.clear();
                    getStories("http://100.26.132.75/story/search/" + searchBar.getText().toString());
                    return true;
                }
                return false;
            }
        });


        drawer = findViewById(R.id.drawer_layout);
        //drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
      
        email = SaveSharedPreference.getEmail(ExploreActivity.this);
        username = SaveSharedPreference.getUserName(ExploreActivity.this);

        BottomNavigationView bottomNavigationView = (BottomNavigationView)findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.explore);

        NavigationView navigationView = findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        showEmail = headerView.findViewById(R.id.showEmail);
        showUsername = headerView.findViewById(R.id.showUsername);
        showEmail.setText(email);
        showUsername.setText(username);



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
                        SaveSharedPreference.clearUser(ExploreActivity.this);
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.side_delete_user:
                        new AlertDialog.Builder(ExploreActivity.this)
                                .setTitle("Are you sure")
                                .setMessage("Your account and stories will be permanently deleted")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        DeleteUser deleteUser = new DeleteUser();
                                        deleteUser.deleteUserRequest("http://100.26.132.75/user/id/" + SaveSharedPreference.getUserId(ExploreActivity.this), ExploreActivity.this);
                                        finish();
                                        overridePendingTransition(0, 0);
                                    }
                                })

                                .setNegativeButton(android.R.string.no, null)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();

                        return true;
                }
                return false;
            }
        });

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.menu:

                        if(!drawer.isDrawerOpen(GravityCompat.START)) drawer.openDrawer(GravityCompat.START);
                        else drawer.closeDrawer(GravityCompat.END);
                        return true;

                    case R.id.map_view:
                        startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.camera:
                        startActivity(new Intent(getApplicationContext(), cameraActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return true;

                    case R.id.explore:
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

    private void addItems(int loadCount) {
        if(!allStoriesLoaded) {
            getStories("http://100.26.132.75/story?number=10&offset=" + loadCount * 10);
        }
    }

    private void getStories(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(onSearch) {
                                stories = response.getJSONArray("searchStories");
                            }
                            else {
                                stories = response.getJSONArray("stories");
                            }
                            Log.d("mytag", "stories: " + stories);

                            if(stories.isNull(0)) {
                                allStoriesLoaded = true;
                                if(!onSearch) {
                                    listViewItems.remove(currentTotalItemCount - 1);
                                }
                                adapter.notifyDataSetChanged();
                            }
                            else {
                                parseJSON(stories);
                                flag_loading = false;
                            }
                        }
                        catch(JSONException e) {
                            Log.d("mytag", "" + e);
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("mytag", "" + error);
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void parseJSON(JSONArray json) {

        for(int i = 0; i < json.length(); i++) {
            try {
                story = json.getJSONObject(i);
            } catch (JSONException e) {
                Log.d("mytag", "" + e);
            }
            try {
                description = story.getString("description");
                image = story.getString("image");
                postersUsername = story.getString("username");
                lat = story.getString("lat");
                lng = story.getString("lng");
                isoTime = story.getString("timestamp"); //tässä haetaan timestamp ISO 8601 muodossa
                storyId = story.getString("storyid");

            } catch (JSONException e) {
                Log.d("mytag", "" + e);
            }
            byte[] decodedString = Base64.decode(image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

            try {
                Date date = inputFormat.parse(isoTime);
                niceDateStr = (String) DateUtils.getRelativeTimeSpanString(date.getTime() ,
                        Calendar.getInstance().getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            ListViewItem l = new ListViewItem(description, decodedByte, postersUsername, lat, lng, niceDateStr);
            listViewItems.add(l);
            storyIdList.add(storyId);
        }

        if(!onSearch) {
            ListViewItem l = new ListViewItem("", null, "Loading...", "", "", "");
            listViewItems.add(l);
        }

        arrayAdapt();
        findViewById(R.id.loading).setVisibility(View.GONE);
    }

    private void arrayAdapt() {
        if(firstItemLoaded == 0) {
            adapter = new CustomListView(this, listViewItems);
            listView.setAdapter(adapter);
            firstItemLoaded = 1;
        }
        else {
            listViewItems.remove(currentTotalItemCount - 1);
        }
        adapter.notifyDataSetChanged();
    }
}