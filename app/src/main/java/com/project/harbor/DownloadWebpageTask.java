package com.project.harbor;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DownloadWebpageTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... urls){
        // params comes from the execute() call:
        // params[0] is the url.
        try{
            return downloadUrl(urls[0]);
        }catch(IOException e){
            return "Unable to retrieve URL (maybe invalid).";
        }
    }

    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result){}

    /**
     * Download a page from url
     * @param myurl where download page
     * @return the text containing on the web page
     * @throws IOException when failed
     */
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        try{
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000/* milliseconds */);
            conn.setConnectTimeout(15000/* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);// Starts the query
            conn.connect();
            //int response = conn.getResponseCode();
            is = conn.getInputStream();
            // Convert the InputStream into a string
            return contentAsString(is);
            // Makes sure that the InputStream is closed after the app is finished using it.
        }
        finally{
            if(is !=null)  is.close();
        }
    }

    /**
     * Convert a inputStream into a string
     * @param is the inputStream to convert
     * @return the string
     */
    private static String contentAsString(InputStream is) {

        BufferedReader reader = new BufferedReader(new
                InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            Log.e("CONVERSION", "Failed to read string from input string");
        }
        return sb.toString();
    }
}