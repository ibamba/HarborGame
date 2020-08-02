package com.project.harbor;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Objects;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final int GAME_REQUEST_CODE = 13;

    private User user;
    private ArrayList<String> stats, levels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        user = (User) (getIntent().getSerializableExtra("USER"));
        levels = (getIntent().getStringArrayListExtra("LEVELS"));
        stats = (getIntent().getStringArrayListExtra("STATS"));
        Objects.requireNonNull(mapFragment).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Marker[] markers = new Marker[levels.size()];
        String name;
        double longitude, latitude;
        int i=0;
        for(String str : levels) {
            String[] data = str.split(",");
            if(data.length == 3) {
                name = data[0];
                longitude = Double.parseDouble(data[1]);
                latitude = Double.parseDouble(data[2]);
                // Add markers
                markers[i] = googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        .draggable(true)
                        .alpha(0.7f)
                        .visible(true)
                        .title(name)
                        .snippet("Level "+(i+1)+" "+name));
                markers[i].setTag(i+1);
                i++;
            }
        }

        if(i==0) {
            // This might not be happen
            markers = new Marker[1];
            markers[0] = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(48.8566, 2.3522))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .draggable(true)
                    .alpha(0.7f)
                    .visible(true)
                    .title("PARIS")
                    .snippet("Level PARIS, France"));
            markers[i].setTag(1);
        }

        // move the camera to North Atlantic Ocean
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(54.526, -25.2551)));
        googleMap.setOnMarkerClickListener(this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // Retrieve the data from the marker.
        Intent intent;
        Integer tag = (Integer) marker.getTag();
        if (tag != null) {
            intent = new Intent(this, GameActivity.class);
            intent.putExtra("USER", user);
            // we send : "levelName,level,totalLevel,nbToPark"
            intent.putExtra("LEVEL", marker.getTitle()+","+tag+","+levels.size()+","+tag*10);
            launchGame(tag, marker.getTitle(), tag*10, intent);
        }
        return false;
    }

    /**
     * Display level info then launch the game
     * @param level_diff    : difficulty of the level
     * @param level_name    : name of the level
     * @param level_nb_park : nb boat to park in this level
     * @param intent        : the game to launch
     */
    public void launchGame(int level_diff, String level_name, int level_nb_park, final Intent intent) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.info);
        alert.setMessage(getString(R.string.level_name) + " : " + level_name + "\n" +
                getString(R.string.level_diff) + " : " + level_diff + "/"+levels.size()+"\n" +
                getString(R.string.level_nb_park) + " : " + level_nb_park + "\n\n" +
                getString(R.string.level_confirm));
        alert.setIcon(android.R.drawable.ic_dialog_info);
        alert.setCancelable(false);

        alert.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // launching the game
                        dialog.cancel();
                        // stop background music
                        stopService(new Intent(getApplicationContext(), BackgroundMusicService.class));
                        // start activity
                        startActivityForResult(intent, GAME_REQUEST_CODE);

                    }
                });

        alert.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GAME_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(new Intent(getApplicationContext(), BackgroundMusicService.class));
            user = (User) data.getSerializableExtra("USER");
            stats.add(0, data.getStringExtra("STAT"));
        }

    }

    /**
     * To update user info and stats in MainActivity
     */
    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("USER", user);
        returnIntent.putStringArrayListExtra("STATS", stats);
        setResult(RESULT_OK, returnIntent);
        finish();
    }
}