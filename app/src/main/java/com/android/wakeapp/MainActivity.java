package com.android.wakeapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.Manifest;
import android.app.TimePickerDialog;
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

public class MainActivity extends AppCompatActivity {
    int min, hour, vorbereitung;
    Button btnAnkunft;
    Button btnGPS;
    EditText minVor;
    EditText gpsText;
    FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        btnGPS = findViewById(R.id.buttonGPS);
        btnGPS.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
            } else {
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
                            Toast.makeText(MainActivity.this, addresses.get(0).getAdminArea(), Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

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
}