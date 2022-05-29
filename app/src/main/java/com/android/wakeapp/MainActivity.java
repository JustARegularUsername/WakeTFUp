package com.android.wakeapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

// Wir nutzen Google Play Services, also ist ein Google Account notwendig!!
// Wir könnten auch builtin services wie LocationManager nutzen, aber Google bietet eine tolle api
// Wenn kein echtes Handy, dann per Device Manager ein Android 11+ device mit Google Play konfigurieren

public class MainActivity extends AppCompatActivity {
    private int min, hour, vorbereitung;
    private Button btnAnkunft;
    private Button btnGPS;
    private Button btnMaps;
    private EditText minVor;
    private EditText gpsText;
    private EditText mapsText;
    private Intent intent;


    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapsText = findViewById(R.id.editTextTextPostalAddress);

        ActivityResultLauncher<Intent> mapsActivityErgebnis = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Bundle res = intent.getExtras();
                            if (res != null) {
                                mapsText.setText(res.getString("adresse"));
                            } else {
                                mapsText.setText(null);
                            }
                        }
                    }
                });

        getLocationPermission();
        if (isAPIOk()) {
            // Maps Knopf
            btnMaps = findViewById(R.id.mapsBtn);
            btnMaps.setOnClickListener(v -> {
                intent = new Intent(this.getApplicationContext(), MapsActivity.class);
                mapsActivityErgebnis.launch(intent);
            });
        }

        // GPS Knopf
        btnGPS = findViewById(R.id.buttonGPS);
        btnGPS.setOnClickListener(v -> {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null) {
                        try {
                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.GERMANY);
                            List<Address> addresses = geocoder
                                    .getFromLocation(
                                            location.getLatitude(),
                                            location.getLongitude(),
                                            1);
                            gpsText = findViewById(R.id.textAdresse);
                            gpsText.setText(addresses.get(0).getAddressLine(0));
                            Toast.makeText(MainActivity.this, addresses.get(0).getAdminArea(), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });

        // Ankuft Button / Time Picker overlay
        btnAnkunft = findViewById(R.id.timeButton);
        btnAnkunft.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minute) -> {
                min = minute;
                hour = hourOfDay;
                TextView ankunft = findViewById(R.id.timeText);
                if (Integer.toString(hour).toCharArray().length < 2 && Integer.toString(min).toCharArray().length < 2) {
                    ankunft.setText(getString(R.string.uhrPrefix24_1, hour, min));
                } else if (Integer.toString(hour).toCharArray().length == 2 && Integer.toString(min).toCharArray().length < 2) {
                    ankunft.setText(getString(R.string.uhrPrefix24_2, hour, min));
                } else if (Integer.toString(hour).toCharArray().length < 2 && Integer.toString(min).toCharArray().length == 2) {
                    ankunft.setText(getString(R.string.uhrPrefix24_3, hour, min));
                } else {
                    ankunft.setText(getString(R.string.uhrPrefix24_4, hour, min));
                }
            };
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    MainActivity.this, onTimeSetListener, hour, min, true);
            timePickerDialog.setTitle("Setze Ankunftszeit");
            timePickerDialog.show();
        });

        // Vorbereitung Text Listener
        minVor = findViewById(R.id.vorbereitungMin);
        minVor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                vorbereitung = Integer.parseInt(String.valueOf(minVor.getText()));
            }

            @Override
            public void afterTextChanged(Editable s) {
                Toast.makeText(getApplicationContext(),
                        "Vorbereitung: " + s.toString() + " Minuten",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isAPIOk() {
        int test = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if (test == ConnectionResult.SUCCESS) {
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(test)) {
            Dialog dia = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, test, 9001);
            assert dia != null;
            dia.show();
        } else {
            Toast.makeText(this, "Map Request geht bei dir nicht", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Boolean mLocationPermissionsGranted = true;
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}