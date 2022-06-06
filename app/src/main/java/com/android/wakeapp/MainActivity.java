package com.android.wakeapp;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

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
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.Time;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// Wir nutzen Google Play Services, also ist ein Google Account notwendig!!
// Wir könnten auch builtin services wie LocationManager nutzen, aber Google bietet eine tolle api
// Wenn kein echtes Handy, dann per Device Manager ein Android 11+ device mit Google Play konfigurieren

public class MainActivity extends AppCompatActivity {
    private int min, hour, vorbereitung;
    private EditText minVor;
    private EditText gpsText;
    private EditText mapsText;
    private Intent intent;
    private RadioButton opnv;
    private Address startAd;
    private Address endAd;
    private TextView ankunft;
    private TextView aufstehen;
    //private ActivityResultLauncher<Intent> mapsActivityErgebnis; <-- Macht faxen lol/nutzen onActivityResult


    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mapsText = findViewById(R.id.editTextTextPostalAddress);
        opnv = findViewById(R.id.opnvBtn);
        Button berechnen = findViewById(R.id.berechnenButton);
        ankunft = findViewById(R.id.timeText);
        aufstehen = findViewById(R.id.berechneteUhrzeit);

        berechnen.setOnClickListener(v -> {
            if (mapsText.getText() == null
                    || mapsText.getText().toString().length() <= 3
                    || gpsText.getText() == null
                    || gpsText.getText().toString().length() <= 3
            ) {
                Toast.makeText(getApplicationContext(),"Bitte beide Adressen fuellen!", Toast.LENGTH_SHORT).show();
            } else {
                if (ankunft.getText().equals("") || vorbereitung <= 0) {
                    Toast.makeText(getApplicationContext(), "Bitte Zeiten einstellen!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Wird berechnet (kann dauern).", Toast.LENGTH_SHORT).show();
                    Time ankunft = new Time(hour, min, 0);
                    BVGAPI bvgapi = opnv.isChecked()
                            ? new BVGAPI(startAd, endAd,true)
                            : new BVGAPI(startAd, endAd,false);
                    try {
                        long fahrZeitMilli = TimeUnit.MINUTES.toMillis(bvgapi.getGesamtDauer());
                        long vorbereitungMilli = TimeUnit.MINUTES.toMillis(vorbereitung);
                        ankunft.setTime(ankunft.getTime() - (fahrZeitMilli + vorbereitungMilli));
                        aufstehen.setText(getString(R.string.aufstehen, ankunft.toString()));
                    }
                    catch (IOException | ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // Neue Version hat auf meinem Testgerät Probleme/ Verwenden stattdessen onActivityResult

//        mapsActivityErgebnis = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result, Intent test) {
//                        if (result.getResultCode() == Activity.RESULT_OK) {
//                            Bundle res = intent.getExtras();
//                            if (res != null) {//res != null) {
//                                mapsText.setText(intent.getStringExtra("adresseString"));
//                            } else {
//                                mapsText.setText("Test");
//                            }
//                        }
//                    }
//                });

        getLocationPermission();
        if (isAPIOk()) {
            // Maps Knopf
            Button btnMaps = findViewById(R.id.mapsBtn);
            btnMaps.setOnClickListener(v -> {
                intent = new Intent(MainActivity.this, MapsActivity.class);
                //mapsActivityErgebnis.launch(intent);
                startActivityForResult(intent, 1);
            });
        }

        // GPS Knopf
        Button btnGPS = findViewById(R.id.buttonGPS);
        btnGPS.setOnClickListener(v -> {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
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
                        startAd = addresses.get(0);
                        Toast.makeText(MainActivity.this, addresses.get(0).getAdminArea(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        // Ankuft Button / Time Picker overlay
        Button btnAnkunft = findViewById(R.id.timeButton);
        btnAnkunft.setOnClickListener(v -> {
            TimePickerDialog.OnTimeSetListener onTimeSetListener = (view1, hourOfDay, minute) -> {
                min = minute;
                hour = hourOfDay;
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
                if (minVor.getText().length() >= 1) {
                    vorbereitung = Integer.parseInt(String.valueOf(minVor.getText()));
                } else {
                    vorbereitung = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                Toast.makeText(getApplicationContext(),
                        "Vorbereitung: " + vorbereitung + " Minuten",
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
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                assert data != null;
                Bundle res = data.getExtras();
                if (res != null) {
                    mapsText.setText(res.getString("adresseString"));
                    endAd = (Address) res.get("adresseObject");
                } else {
                    mapsText.setText(getString(R.string.mapsFail));
                }
            }
        }
    }
}