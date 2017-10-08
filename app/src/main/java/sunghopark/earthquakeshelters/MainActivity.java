package sunghopark.earthquakeshelters;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.*;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.text.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String QUERY_REQUEST = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson";
    private static final String COUNT_REQUEST = "https://earthquake.usgs.gov/fdsnws/event/1/count?";
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"; //iso format
    private static final TimeZone utc = TimeZone.getTimeZone("UTC"); //utc time
    private static final int GAP_TIME_FIVE_MIN = 300 * 1000; //in miliseconds
    private static final int GAP_TIME_TEN_MIN = 3600 * 1000; //in miliseconds
    private static final int GAP_TIME_HOUR = 3600 * 1000; //in miliseconds
    private static final int GAP_TIME_DAY = 86400000;
    private static final String MIN_MAGNITUDE_ONE = "&minmagnitude=1";
    private static final String MIN_MAGNITUDE_THREE = "&minmagnitude=3";
    private static final String MIN_MAGNITUDE_FOUR_POINT_FIVE = "&minmagnitude=5";
    private static final String MIN_MAGNITUDE_FIVE = "&minmagnitude=5";
    private static final String MAX_RADIUS_ONE = "&maxradiuskm=50";
    private static final String MAX_RADIUS_TWO = "&maxradiuskm=500";
    private static final String MAX_RADIUS_THREE = "&maxradiuskm=5000";
    private static final String MAX_RADIUS_FOUR = "&maxradiuskm=10000";
    private static final String USER_AGENT = "Chrome/4.0.249.0";
    private static final Location BERKELEYLOCATION = new Location("providername");

    private LocationManager locationManager;
    private ToggleButton toggle;

    //Initial values of Berkeley's Location
    protected void init() {
        BERKELEYLOCATION.setLatitude(37.8716);
        BERKELEYLOCATION.setLongitude(-122.2727);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        toggle = (ToggleButton) findViewById(R.id.toggleButton);
    }

    //Determines whether to enable or disable EarthQuake Alert Feature of the App
    protected void EarthQuakeAlert(View view) {
        if (toggle.isChecked()) {
            DisableEarthQuakeAlert();
        } else {
            EnableEarthQuakeAlert();
        }
    }

    /*
    Enables the EarthQuakeAlert feature of the appplication
    Gets Last Known Location and checks if your Last Known Location is in the EarthQuake Zone
    If so Alert User
    */
    protected void EnableEarthQuakeAlert()  {
        Location myLocation = getLastKnownLocation();
        if (myLocation == null) {
            return;
        }
        try {
            if (inEarthQuakeZone(myLocation.getLatitude(), myLocation.getLongitude())) {
                AlertUser();
            }
        } catch (Exception e) {
            return;
        }
    }


    //TODO: WRITE DISABLE EARTHQUAKE APP;
    protected void DisableEarthQuakeAlert() {

    }


    /*
    Function that Gets Last Known Location
    First requests permission to access coarse and fine location
    If denied then app will not run
    If accepted then GPS Location will be returned
    */
    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION }, 0);
            return null;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            return location;
        } else {
            return null;
        }
    }

    /*
    Checks if user is in the EarthQuake Zone
    Makes a HTTP-GET Request Connection to the USGS earthquake server
    If Request returns HTTP STATUS 200 (HTTP_OK) then read in the contents of the connection
    Else return false;
    https://earthquake.usgs.gov/fdsnws/event/1/query?format=xml&starttime=2014-01-01&endtime=2014-01-02&minmagnitude=5
    Must run asynchronous threads or else the HTTP request will not run.
     */
    protected boolean inEarthQuakeZone(double latitude, double longitude) {
        AsyncTask<Double, Void, Boolean> async = new AsyncTask<Double, Void, Boolean>() {
            protected Boolean doInBackground(Double ... locations) {
                try {
                    Date endDate = new Date();
                    Date startDate = new Date(System.currentTimeMillis() - GAP_TIME_DAY); //can change to GAP_TIME
                    SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
                    sdf.setTimeZone(utc);
                    String startTime = sdf.format(startDate);
                    String endTime = sdf.format(endDate);
                    String lat = "&latitude=" + locations[0];
                    String lon = "&longitude=" + locations[1];
                    String request = COUNT_REQUEST + "&starttime=" + startTime + "&endtime=" + endTime + MIN_MAGNITUDE_FOUR_POINT_FIVE
                            + lat + lon + MAX_RADIUS_FOUR;

                    URL url = new URL(request);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", USER_AGENT);
                    int responseCode = connection.getResponseCode();

                    //System.out.println("GET Response Code :: " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) { // success
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        int numberOfEarthQuakes = Integer.parseInt(in.readLine());
                        in.close();
                        //System.out.println("RESPONSE CODE: " + responseCode + " NUMBER OF EARTHQUAKES: " + numberOfEarthQuakes);
                        return numberOfEarthQuakes != 0;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        };

        try {
            Double[] location = {latitude, longitude};
            Boolean ret = async.execute(location).get();
            return ret;
        } catch (Exception e){
            return false;
        }
    }

    /*protected boolean inEarthQuakeZone(double latitude, double longitude) throws IOException {
        try {
            Date endDate = new Date();
            Date startDate = new Date(System.currentTimeMillis() - GAP_TIME_DAY); //can change to GAP_TIME
            SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
            sdf.setTimeZone(utc);
            String startTime = sdf.format(startDate);
            String endTime = sdf.format(endDate);
            String lat = "&latitude=" + latitude;
            String lon = "&longitude=" + longitude;
            String request = COUNT_REQUEST + "&starttime=" + startTime + "&endtime=" + endTime + MIN_MAGNITUDE_FOUR_POINT_FIVE
                    + lat + lon + MAX_RADIUS_FOUR;

            URL url = new URL(request);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            //connection.setRequestProperty("User-Agent", USER_AGENT);
            int responseCode = connection.getRequestMethod();

            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                int numberOfEarthQuakes = Integer.parseInt(in.readLine());
                in.close();
                System.out.println("RESPONSE CODE: " + responseCode + " NUMBER OF EARTHQUAKES: " + numberOfEarthQuakes);
                return numberOfEarthQuakes != 0;
            } else {
                //System.out.println("FAILED: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }*/

    protected void AlertUser() {

    }
}
