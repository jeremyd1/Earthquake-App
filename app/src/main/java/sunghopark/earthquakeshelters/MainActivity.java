package sunghopark.earthquakeshelters;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    static double lat;
    static double lon;
    static String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    // For Scalability, we can access a database for shelters and add them

    /** Called when the user taps Shelter 1*/
    public void goToShelter_1(View view) {
        Intent intent = new Intent(this, shelter.class);
        // Tang Center
        lat = 37.8676;
        lon = -122.2642;
        name = "Tang Center";
        startActivity(intent);
    }

    public void goToShelter_2(View view) {
        Intent intent = new Intent(this, shelter.class);
        // Berkeley Clinic
        lat = 37.8677;
        lon = -122.2617;
        name = "Berkeley Clinic";
        startActivity(intent);
    }

    public void goToShelter_3(View view) {
        Intent intent = new Intent(this, shelter.class);
        // Hospital
        lat = 37.8555;
        lon = -122.2573;
        name = "Berkeley Hospital";
        startActivity(intent);
    }

}
