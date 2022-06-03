package com.android.wakeapp;

import android.location.Address;
import android.util.Log;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;


public class BVGAPI {
    private Address[] adressen = new Address[2];
    private String journeyUri;
    private String[] locationUri;
    private boolean opvnOderNicht;
    private String[] locIDs = new String[2];

    BVGAPI(Address start, Address ende, boolean opvnOderNicht) {
        this.adressen[0] = start;
        this.adressen[1] = ende;
        this.opvnOderNicht = opvnOderNicht;
        this.journeyUri = createJorneyUri();
    }

    private String replaceSpecial(String adresse) {
        return adresse.replace(" ", "+").replace("ß", "ss");
    }

    private String createLocationUri(Address address) {
        return "https://v5.bvg.transport.rest/locations?query="
                + replaceSpecial(address.getAddressLine(0))
                + "&results=1";
    }

    private String getLocationID(String locUri) throws IOException {
        URL url = new URL(locUri);
        JsonObject jsonObject;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int resultCode = connection.getResponseCode();

        if (resultCode != 200)
        {
            throw new MalformedURLException("Fehler!: " + resultCode);
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

            Log.i("Loc. ID: ", jsonObject.get("id").toString());
            Log.i("Array Groesse: ", Integer.toString(jsonArray.size()));

        }
        return Objects.requireNonNull(jsonObject.get("id")).toString();
    }

    private String createJorneyUri() {
        return "https://v5.bvg.transport.rest/journeys?"
                + "from.id="
                + locIDs[0]
                + "&from.latitude="
                + adressen[0].getLatitude()
                + "&from.longitude="
                + adressen[0].getLongitude()
                + "&to.latitude="
                + adressen[1].getLatitude()
                + "&to.longitude="
                + adressen[1].getLongitude()
                + "&from.name="
                + replaceSpecial(adressen[1].getAddressLine(0));
    }

    public int getGesamtDauer() throws IOException {
        URL url = new URL(journeyUri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        for (int i = 0; i < adressen.length; i++) {
            this.locIDs[i] = getLocationID(createLocationUri(adressen[i]));
        }
        connection.connect();

        int resultCode = connection.getResponseCode();

        if (resultCode != 200)
        {
            throw new MalformedURLException("Fehler!: " + resultCode);
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
        }
        return 0;
    }


    public Address[] getAdressen() {
        return adressen;
    }

    public void setAdressen(Address[] adressen) {
        this.adressen = adressen;
    }

    public String getJourneyUri() {
        return journeyUri;
    }

    public void setJourneyUri(String journeyUri) {
        this.journeyUri = journeyUri;
    }

    public boolean isOpvnOderNicht() {
        return opvnOderNicht;
    }

    public void setOpvnOderNicht(boolean opvnOderNicht) {
        this.opvnOderNicht = opvnOderNicht;
    }
}
