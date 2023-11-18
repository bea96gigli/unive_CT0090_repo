package com.example.fillndrive;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.SupportMapFragment;

import com.example.fillndrive.databinding.ActivityMapsBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private DBHelper db;
    private SQLiteDatabase dbConnection;
    private GoogleMap googleMap;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private EditText searchEditText;
    private Polyline currentPolyline;
    private LatLng currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchEditText = findViewById(R.id.search_edit_text);
        Button searchButton = findViewById(R.id.search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performSearch();
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void performSearch() {
        String location = searchEditText.getText().toString();
        if (location != null && !location.equals("")) {
            List<Address> addressList = null;

            // Utilizza un Geocoder per cercare l'indirizzo
            Geocoder geocoder = new Geocoder(MapsActivity.this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);

                // Sposta la camera alla posizione dell'indirizzo trovato
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                // Mostra un messaggio di errore se l'indirizzo non viene trovato
                Toast.makeText(MapsActivity.this, "Indirizzo non trovato", Toast.LENGTH_SHORT).show();
            }
        }




    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Controlla se ci sono i permessi per la geolocalizzazione
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {

            // Richiede i permessi
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            this.googleMap.setMyLocationEnabled(true);


            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {

                    // Trova la lista delle stazioni di rifornimento presenti nel comune della posizione
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    Address address = getAddressFromLatLong(location.getLatitude(), location.getLongitude());
                    String comune = address.getLocality();
                    List<StazioneDiRifornimento> listaStazioniByComune = getListaStazioni(comune.toUpperCase());

                    //TODO: da capire di quanto deve essere il raggio. Momentaneamente posto a 7 km.
                    List<StazioneDiRifornimento> listaStazioniIn7Km = getListaStazioniIn7Km(listaStazioniByComune, location.getLatitude(), location.getLongitude());

                    // TODO: calcolare l'indice di convenienza e ordinare la listaStazioni in modo da colorare propriamente i marker nel metodo sotto

                    //Crea i marker sulla mappa corrispondenti alle stazioni di rifornimento trovate
                    createMarkers(listaStazioniIn7Km);

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 14));
                }
           });
        }

            googleMap.setOnMarkerClickListener(marker -> {
                    showMarkerInformation(marker);
                    return true;
            });

        }

    private double calculateDistanceToMarker(LatLng markerCoordinates) {
        // Check if the app has the necessary location permissions
        if (ContextCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return -1; // Return -1 to indicate that the permission is not granted
        }

        // Get the current location
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {

                LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

                double distance = calculateDistance(origin.latitude, origin.longitude, markerCoordinates.latitude, markerCoordinates.longitude);

                // Now 'distance' contains the distance in kilometers
                Toast.makeText(MapsActivity.this, "Distanza al marker: " + distance + " km", Toast.LENGTH_SHORT).show();
            }
        });

        return 0; // Placeholder value, you can replace it with a meaningful value or handle it differently
    }

    private void showMarkerInformation(Marker marker) {
        LatLng coordinates = marker.getPosition();
        String title = marker.getTitle();
        String snippet = marker.getSnippet();
        String info = "custom info";
        calculateDistanceToMarker(coordinates);// solo per testare la funzione
        CustomMarkerInfoFragment infoFragment = CustomMarkerInfoFragment.newInstance(title, snippet, info, coordinates);

        // Passa l'istanza di GoogleMap al fragment
        infoFragment.setGoogleMap(googleMap);

        // Mostra il fragment
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, infoFragment)
                .addToBackStack(null)
                .commit();
    }




    private Address getAddressFromLatLong(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0);
            }
        } catch (IOException e) {
            Toast.makeText(this, "Impossibile ottenere l'indirizzo", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private List<StazioneDiRifornimento> getListaStazioniIn7Km(List<StazioneDiRifornimento> allStazioni, double userLat, double userLng) {
        List<StazioneDiRifornimento> stazioniInRadius = new ArrayList<>();

        for (StazioneDiRifornimento stazione : allStazioni) {
            double stazioneLat = Double.parseDouble(stazione.getLatitudine());
            double stazioneLng = Double.parseDouble(stazione.getLongitudine());

            // Calcola la distanza tra l'utente e la stazione
            double distance = calculateDistance(userLat, userLng, stazioneLat, stazioneLng);

            // Aggiunge la stazione solo se è entro i 7 km
            if (distance <= 7.0) {
                stazioniInRadius.add(stazione);
            }
        }
        return stazioniInRadius;
    }

    /**
     * Haversine formula to calculate distance between two points on a sphere.
     * @param userLat
     * @param userLng
     * @param stationLat
     * @param stationLng
     * @return
     */
    private double calculateDistance(double userLat, double userLng, double stationLat, double stationLng) {
        double earthRadius = 6371; // Radius of the Earth in kilometers

        double latDiff = Math.toRadians(stationLat - userLat);
        double lngDiff = Math.toRadians(stationLng - userLng);

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(stationLat))
                * Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Distance in kilometers
    }

    private void createMarkers(List<StazioneDiRifornimento> listaStazioni) {
        for (StazioneDiRifornimento stazione : listaStazioni) {
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(Double.parseDouble(stazione.getLatitudine()), Double.parseDouble(stazione.getLongitudine())))
                    .title(String.valueOf(stazione.getPrezzo()))
                    .snippet(stazione.getBandiera())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))); //TODO: cambiare colore icona marker
        }
    }

    public List<StazioneDiRifornimento> getListaStazioni(String comune) {
        List<StazioneDiRifornimento> listaStazioni = new ArrayList<>();
        db = DBHelper.getInstance(this);

        int currentDay = DateUtility.getCurrentDay(this);
        int lastDay = DateUtility.getLastDay(this);

        if(lastDay != currentDay) {
            try {
                db.waitForUpdate(); // attende che entrambi i thread della pool siano terminati

                // Aggiorna la data nelle preferenze condivise
                SharedPreferences.Editor editor = DateUtility.getSharedPreferences(MapsActivity.this).edit();
                editor.putInt("day", currentDay);
                editor.commit();

            } catch (InterruptedException e) {
                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
            }
        }

        // Now, it's safe to query the database
        dbConnection = db.getReadableDatabase();

        String query = "SELECT s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine, MIN(c.prezzo) AS minPrezzo " +
                "FROM stazioni s NATURAL JOIN carburanti c " +
                "WHERE s.comune = ? AND descCarburante LIKE 'Benzina%' " +
                "GROUP BY s.IdImpianto, s.bandiera, s.comune, s.latitudine, s.longitudine";
        Cursor cursor = dbConnection.rawQuery(query, new String[]{comune});

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String id = cursor.getString(0);
                    String bandiera = cursor.getString(1);
                    String comuneFromDb = cursor.getString(2);
                    String latitudine = cursor.getString(3);
                    String longitudine = cursor.getString(4);
                    double prezzo = cursor.getDouble(5);

                    listaStazioni.add(new StazioneDiRifornimento(Integer.parseInt(id), bandiera, comuneFromDb, prezzo, latitudine, longitudine));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        dbConnection.close();
        return listaStazioni;
    }

    /**
     * Metodo che avvia la navigazione verso una destinazione utilizzando le API di Google.
     *
     * @param destination
     */
    private void navigateToMarkerLocation(LatLng destination) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + destination.latitude + "," + destination.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps"); // Specify the Google Maps app package

        // Check if the Google Maps app is installed
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps non installato", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Metodo che chiede all'utente il permesso alla localizzazione del dispositivo.
     *
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "I permessi sono necessari", Toast.LENGTH_SHORT).show();
            }
            else {
                Intent intent = new Intent(MapsActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        }
    }
}
