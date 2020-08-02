package com.project.harbor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.ViewModel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


class MainModel extends ViewModel {

    // Web link where result are stored
    private static final String RESULT_LINK
            = "https://www.lrde.epita.fr/~renault/teaching/ppm/2019/results.txt";

    // Web link where levels are stored
    private static final String LEVELS_LINK
            = "https://www.lrde.epita.fr/~renault/teaching/ppm/2019/levels.txt";

    private static final String USER_FILE = "userInfo.txt", USER_STATS_FILE = "userStats.txt";

    /**
     * Save a serializable user
     * @param user : the user to save
     * @param context : the context
     */
    void saveUser(User user, Context context) {
        try {
            //we save in the app dir
            File filesDir = context.getFilesDir();

            //creates files and dir if necessary
            if(!filesDir.exists()) {
                if(!filesDir.mkdir()) throw new IOException("Failed to make user dir in save user");
            }
            File file = new File(filesDir, USER_FILE);
            if(!file.exists()) {
                if(!file.createNewFile())
                    throw new IOException("Failed to make user file in save user");
            }

            //saving the user
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));
            oos.writeObject("USER INFO"); //to detect bad format of the file when loading a user
            oos.writeObject(user);
            oos.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    /**
     * Load a serializable user
     * @param context : the context
     * @return the user or null if an error occurred when loading
     */
    User loadUser(Context context) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(new File(context.getFilesDir(), USER_FILE))));
            //we detect first if the format of the file is the good
            if(!ois.readObject().equals("USER INFO")) {
                Log.e("Exception",
                        "Bad format when loading data from File : bad header in user" );
                return null;
            }
            Object user = ois.readObject();
            if(!(user instanceof User)) {
                Log.e("Exception",
                        "Bad format when loading data from File : bad type of user" );
                return null;
            }
            ois.close();
            return (User)user;
        }
        catch (Exception e) {
            Log.e("Exception", "File read failed: " + e.toString());
            return null;
        }
    }


    /**
     * Save user stats
     * @param _stats : the stats to save
     * @param context : the context
     */
    void saveUserStats(ArrayList<String> _stats, Context context) {
        try {

            // We remove from the list stats that fetched from the web
            ArrayList<String> stats = new ArrayList<>();
            for(String line : _stats) {
                if(line.charAt(0) != '*')
                    stats.add(line);
            }

            //we save in the app dir
            File filesDir = context.getFilesDir();

            //creates files and dir if necessary
            if(!filesDir.exists()) {
                if(!filesDir.mkdir()) throw new IOException("Failed to make user dir in save stats");
            }
            File file = new File(filesDir, USER_STATS_FILE);
            if(!file.exists()) {
                if(!file.createNewFile()) throw new IOException("Failed to make stats file");
            }

            //saving the user
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file)));
            oos.writeObject("USER STATS"); //to detect bad format of the file when loading a user
            oos.writeObject(stats);
            oos.close();
        }
        catch (Exception e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    /**
     * Load a serializable user
     * @param context : the context
     * @param result : where to save loaded data
     * @return true if load success
     */
    private boolean loadUserStats(ArrayList<String> result, Context context) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(new File(context.getFilesDir(), USER_STATS_FILE))));
            //we detect first if the format of the file is the good
            if(!ois.readObject().equals("USER STATS")) {
                Log.e("Exception",
                        "Bad format when loading data from File : bad header in stats");
                return false;
            }
            Object array = ois.readObject();
            if(!(array instanceof ArrayList)) {
                Log.e("Exception",
                        "Bad format when loading data from File : bad type list in stats" );
                return false;
            }
            for(Object obj : (ArrayList) array) {
                if(!(obj instanceof String)) {
                    Log.e("Exception",
                            "Bad format when loading data from File : bad type string in stats");
                    return false;
                }
                result.add((String)obj);
            }
            ois.close();
            return true;
        }
        catch (Exception e) {
            Log.e("Exception", "File read failed: " + e.toString());
            return false;
        }
    }


    /**
     * Load users stats.
     * The locale user stats are loaded from a locale file
     * Others users stats are loaded from a remote file (on internet)
     * @param context : the context
     * @return Stats on an array and true or null and false if download failed
     */
    Pair<ArrayList<String>, Boolean> loadStats(Context context) {
        ArrayList<String> res = new ArrayList<>();
        boolean fetched;
        String data;

        // Fetch other results stats from internet
        try {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo;
            if (connMgr != null) {
                networkInfo = connMgr.getActiveNetworkInfo();
            } else {
                throw new IOException("Failed to make connection");
            }
            if (networkInfo != null && networkInfo.isConnected()) {
                // Network available ...
                // We download data on the website
                data = new DownloadWebpageTask().execute(RESULT_LINK)
                        .get();

                // Parsing the data to string array
                parseData(res, data, "*");// To differ stats fetched from web to user stats
                fetched = true;

            } else {
                // Network not available ...
                Log.e("CONNECTION FAILED","NETWORK UNAVAILABLE :(");
                fetched = false;
            }
        } catch (Exception e) {
            Log.e("LOAD FROM WEB", "Error when loading data from internet"+e.toString());
            fetched = false;
        }

        // Then download user stats from local file
        if(!loadUserStats(res, context))
            fetched = false;

        return new Pair<>(res, fetched);
    }


    /**
     * Load levels from a remote file (on internet)
     * @param context : the context
     * @return Levels on an array and true or null and false if download failed
     */
    Pair<ArrayList<String>, Boolean> loadLevels(Context context) {
        ArrayList<String> res = new ArrayList<>();
        boolean fetched;

        // Fetch levels from internet
        try {
            ConnectivityManager connMgr =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo;
            if (connMgr != null) {
                networkInfo = connMgr.getActiveNetworkInfo();
            } else {
                throw new IOException("Failed to make connection");
            }
            if (networkInfo != null && networkInfo.isConnected()) {
                // Network available ...
                // We download data on the website
                String data = new DownloadWebpageTask().execute(LEVELS_LINK)
                        .get();

                // Parsing the data to string array
                parseData(res, data, "");
                fetched = true;

            } else {
                // Network not available ...
                Log.e("CONNECTION FAILED","NETWORK UNAVAILABLE :(");
                fetched = false;
            }
        } catch (Exception e) {
            Log.e("LOAD FROM WEB", "Error when loading data from internet"+e.toString());
            fetched = false;
        }

        return new Pair<>(res, fetched);
    }

    /**
     * Parse a string containing data separated with ',' to a list of data
     * @param res where to stock the result
     * @param toParse string to parse
     * @param marker a char to mark every stats of this list
     */
    private void parseData(ArrayList<String> res, String toParse, String marker) {
        String[] lines = toParse.split("\n");

        for(String line : lines) {
            if( line.charAt(0) != '#') // Ignore comment
                res.add(marker+" "+line);
        }

    }

}
