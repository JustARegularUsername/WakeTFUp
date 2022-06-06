package com.android.wakeapp;

import android.annotation.SuppressLint;
import android.location.Address;
import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


public class BVGAPI {
    private Address[] adressen = new Address[2];
    private String journeyUri;
    private boolean opvnOderNicht;
    private final String[][] jData = new String[2][3];
    private final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ssXXX";

    BVGAPI(Address start, Address ende, boolean opvnOderNicht) {
        this.adressen[0] = start;
        this.adressen[1] = ende;
        this.opvnOderNicht = opvnOderNicht;
    }

    private String replaceSpecial(String adresse) {
        return adresse
                .replace(" ", "+")
                .replace("ß", "ss")
                .replace("\"", "");
    }

    private String createLocationUri(Address address) {
        return "https://v5.bvg.transport.rest/locations?query="
                + replaceSpecial(address.getAddressLine(0))
                + "&results=1";
    }

    private JsonObject getLocationID(String locUri) throws IOException {
        URL url = new URL(locUri);
        JsonObject jsonObject;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int resultCode = connection.getResponseCode();

        if (resultCode != 200)
        {
            throw new MalformedURLException("HTTP Code Fehler!: " + resultCode);
        }
        else
        {
            StringBuilder streamString = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());

            while (scanner.hasNext()) {
                streamString.append(scanner.nextLine());
            }
            scanner.close();

            JsonReader jsonReader = Json.createReader(new StringReader(streamString.toString()));
            JsonArray jsonArray = jsonReader.readArray();
            jsonObject = jsonArray.getJsonObject(jsonArray.size() - 1);
        }
        return jsonObject;
    }

    private String createJorneyUri(String[][] latLong) {
        if (opvnOderNicht) {
            return "https://v5.bvg.transport.rest/journeys?"
                    + "&from.latitude="
                    + latLong[0][0]
                    + "&from.longitude="
                    + latLong[0][1]
                    + "&from.address="
                    + latLong[0][2]
                    + "&to.latitude="
                    + latLong[1][0]
                    + "&to.longitude="
                    + latLong[1][1]
                    + "&to.address="
                    + latLong[1][2];
        }
        else {
            // Andere URI fuer Auto nehmen spaeter
            return "https://v5.bvg.transport.rest/journeys?"
                    + "&from.latitude="
                    + latLong[0][0]
                    + "&from.longitude="
                    + latLong[0][1]
                    + "&from.address="
                    + latLong[0][2]
                    + "&to.latitude="
                    + latLong[1][0]
                    + "&to.longitude="
                    + latLong[1][1]
                    + "&to.address="
                    + latLong[1][2];
        }
    }

    public long getGesamtDauer() throws IOException, ParseException {
        JsonObject[] jo = new JsonObject[2];
        long zeit = 0;

        for (int i = 0; i < adressen.length; i++) {
            jo[i] = getLocationID(createLocationUri(adressen[i]));
            jData[i][0] = String.valueOf(jo[i].get("latitude"));
            jData[i][1] = String.valueOf(jo[i].get("longitude"));
            jData[i][2] = String.valueOf(jo[i].get("address"));
        }
        journeyUri = replaceSpecial(createJorneyUri(jData));

        URL url = new URL(journeyUri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.connect();

        int resultCode = connection.getResponseCode();
        if (resultCode != 200)
        {
            throw new MalformedURLException("HTTP Code Fehler!: " + resultCode);
        }
        else
        {
            StringBuilder streamString = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());

            while (scanner.hasNext()) {
                streamString.append(scanner.nextLine());
            }
            scanner.close();

            JsonReader jsonReader = Json.createReader(new StringReader(streamString.toString()));
            JsonArray ja = jsonReader
                    .readObject()
                    .getJsonArray("journeys")
                    .get(0)
                    .asJsonObject()
                    .getJsonArray("legs");
            Log.i("Array Laenge ", Integer.toString(ja.size()));


            for (int i = 0; i < ja.size(); i++) {
                JsonObject jsonObject = ja.get(i).asJsonObject();

                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601);
                Date departure = sdf.parse(jsonObject.getString("departure"));
                Date arrival = sdf.parse(jsonObject.getString("arrival"));

                assert arrival != null;
                assert departure != null;
                zeit += ((arrival.getTime() / 1000) / 60) - ((departure.getTime() / 1000) / 60);
            }
        }
        return zeit;
    }
}
