package sunghopark.earthquakeshelters;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity{

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

    private ToggleButton toggleButton;

    static boolean toggle = false;
    static double lat;
    static double lon;
    static String name;
    static Location current;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private static TextToSpeech myTTS;
    public static double currlat;
    public static double currlong;
    Button button;
    private static int REQUEST_CODE_RECOVER_PLAY_SERVICES = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLastLocation = getLastKnownLocation();
        if (mLastLocation != null) {
            currlat = mLastLocation.getLatitude();
            currlong = mLastLocation.getLongitude();
        }

        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);

        boolean inZone = false;

        if (!toggleButton.isChecked()) {
            inZone = inEarthQuakeZone(currlat, currlong);
        }

        if (inZone) {
            AlertUser();
        }
    }


    //Determines whether to enable or disable EarthQuake Alert Feature of the App
    protected void EarthQuakeAlert(View view) {
        if (toggleButton.isChecked()) {
            //DisableEarthQuakeAlert();
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

    public void makeAnoun(Boolean earthquake) {
        if (earthquake == true) {
            button.performClick();
        }
    }

    // For Scalability, we can access a database for shelters and add them

    /** Called when the user taps Shelter 1*/
    public void goToShelter_1(View view) {
        Intent intent = new Intent(this, Shelter.class);
        // Tang Center
        lat = 37.8676;
        lon = -122.2642;
        name = "Tang Center";
        mLastLocation = getLastKnownLocation();
        startActivity(intent);
    }

    public void goToShelter_2(View view) {
        Intent intent = new Intent(this, Shelter.class);
        // Alta Bates Medical Center
        lat = 37.8642;
        lon = -122.2689;
        name = "Alta Bates Medical Center";
        mLastLocation = getLastKnownLocation();
        startActivity(intent);
    }

    public void goToShelter_3(View view) {
        Intent intent = new Intent(this, Shelter.class);
        // Hospital
        lat = 37.8555;
        lon = -122.2573;
        name = "Berkeley Hospital";
        mLastLocation = getLastKnownLocation();
        startActivity(intent);
    }

    // alerts

    public void showAlert(View view){
        AlertDialog.Builder myAlert = new AlertDialog.Builder(this);
        if (!toggle) {
            myAlert.setMessage("Alert has been turned on")
                    .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            toggle = true;
            EarthQuakeAlert(view);
        } else {
            myAlert.setMessage("Alert has been turned off")
                    .setPositiveButton("Done ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            toggle = false;
        }
        myAlert.show();
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
            currlat = location.getLatitude();
            currlong = location.getLongitude();
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

    protected void AlertUser() {
        button = (Button) findViewById(R.id.button5);

        myTTS = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    myTTS.setLanguage(Locale.UK);
                }
            }
        });

        button = (Button) findViewById(R.id.button5);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler = new Handler();
                String toSpeak = "If Indoors, drop, cover your head, and hold on," +
                        "If Outdoors, move to a clear area; stay away from beach";
                Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                myTTS.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        final Handler handler = new Handler();
        makeAnoun(true);
    }


}
