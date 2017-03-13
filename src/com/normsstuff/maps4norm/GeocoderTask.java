/*
 * Copyright 2014 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 package com.normsstuff.maps4norm;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;

/**
 * Based on
 * http://wptrafficanalyzer.in/blog/android-geocoding-showing-user-input
 * -location-on-google-map-android-api-v2/
 *
 * @author George Mathew
 */
public class GeocoderTask extends AsyncTask<String, Void, Address> {

    private final MyMapActivity map;

    public GeocoderTask(final MyMapActivity m) {
        map = m;
    }

    @Override
    protected Address doInBackground(final String... locationName) {
        // Creating an instance of Geocoder class
        Geocoder geocoder = new Geocoder(map.getBaseContext());
        try {
            // Get only the best result that matches the input text
            List<Address> addresses = geocoder.getFromLocationName(locationName[0], 1);
            return addresses != null && !addresses.isEmpty() ? addresses.get(0) : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(final Address address) {
        if (address == null) {
            Toast.makeText(map.getBaseContext(), R.string.no_location_found, Toast.LENGTH_SHORT).show();
        } else {
            map.getMap().animateCamera(CameraUpdateFactory
                    .newLatLngZoom(new LatLng(address.getLatitude(), address.getLongitude()),
                            Math.max(10, map.getMap().getCameraPosition().zoom)));
        }
    }
}
