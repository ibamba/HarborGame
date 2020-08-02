package com.project.harbor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PROFILE_REQUEST_CODE = 10;
    private static final int MAPS_REQUEST_CODE = 11;

    private User user;
    private ArrayList<String> stats;
    private ArrayList<String> levels;
    private MainModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btGame = findViewById(R.id.gotogamebt);
        Button btProfile = this.findViewById(R.id.gotoprofilbt);
        Button btStats = this.findViewById(R.id.gotostatsbt);

        model = new MainModel();

        //We load user data from file
        user = model.loadUser(this);
        if(user == null) { //Loading failed
            final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(R.string.warning);
            alert.setMessage(R.string.load_failed);
            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setCancelable(false);
            alert.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();

            //Default user is created
            user = new User();
        }

        //downloading user stats and other stats from the net
        Pair<ArrayList<String>, Boolean> result = model.loadStats(MainActivity.this);
        Pair<ArrayList<String>, Boolean> level = model.loadLevels(MainActivity.this);
        if(!result.second || result.first.isEmpty() || !level.second || level.first.isEmpty()) {
            // Loaded failed
            final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(R.string.warning);
            alert.setMessage(R.string.load_failed);
            alert.setIcon(android.R.drawable.ic_dialog_alert);
            alert.setCancelable(false);
            alert.setPositiveButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }
        stats = result.first;
        levels = level.first;

        //Launch the GAME
        btGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("USER", user);
                intent.putStringArrayListExtra("LEVELS", levels);
                intent.putStringArrayListExtra("STATS", stats);
                startActivityForResult(intent, MAPS_REQUEST_CODE);
            }
        });

        //Launch the PROFILE with the current user
        btProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                intent.putExtra("USER", user);
                startActivityForResult(intent, PROFILE_REQUEST_CODE);
            }
        });

        //Launch the STATS with the current user
        btStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatsActivity.class);
                intent.putStringArrayListExtra("STATS", stats);
                startActivity(intent);
            }
        });

        startService(new Intent(getApplicationContext(), BackgroundMusicService.class));
    }



    /**
     * To update data for data changes from up level Activities
     * @param requestCode Activity code
     * @param resultCode Data changed or not
     * @param data data to update
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PROFILE_REQUEST_CODE || requestCode == MAPS_REQUEST_CODE) {
            if(resultCode == RESULT_OK){
                user = (User) data.getSerializableExtra("USER");
                if (requestCode == MAPS_REQUEST_CODE) {
                    stats = (data.getStringArrayListExtra("STATS"));
                }
            }
        }
    }


    /**
     * To save data when user exit the app and stop music
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // saving data
        model.saveUser(user, this);
        model.saveUserStats(stats, this);

        // stop background music
        stopService(new Intent(getApplicationContext(), BackgroundMusicService.class));
    }
}