package com.parse.starter;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class DriverViewActivity extends FragmentActivity implements OnMapReadyCallback {

  private GoogleMap mMap;

  Intent intent;

  Double driverLatitude;

  Double driverLongitude;

  Double requestLatitude;

  Double requestLongitude;

  public void acceptRequest(View view) {

    intent = getIntent();

    final String username = intent.getStringExtra("username");

    ParseQuery<ParseObject> query = ParseQuery.getQuery("request");

    query.whereEqualTo("username", username);

    query.findInBackground(new FindCallback<ParseObject>() {

      @Override
      public void done(List<ParseObject> objects, ParseException e) {

        if (e == null) {

          if (objects.size() > 0) {

            for (ParseObject obj : objects) {

              obj.put("requestAccepted", ParseUser.getCurrentUser().getUsername());

              obj.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                  if (e == null) {

                    Intent directionIntent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("http://maps.google.com/maps?saddr=" + driverLatitude + ","
                                    + driverLongitude + "&daddr=" + requestLatitude + "," + requestLongitude));

                    startActivity(directionIntent);

                  }

                }

              });


            }

          }

        }

      }
    });



  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_driver_view);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @Override
  public void onMapReady(GoogleMap googleMap) {

    mMap = googleMap;

    ArrayList<Marker> markers = new ArrayList<>();

    intent = getIntent();

    driverLatitude = intent.getDoubleExtra("driverLatitude", 0);

    driverLongitude = intent.getDoubleExtra("driverLongitude", 0);

    requestLatitude = intent.getDoubleExtra("requestLatitude", 0);

    requestLongitude = intent.getDoubleExtra("requestLongitude", 0);

    LatLng driverLocation = new LatLng(driverLatitude, driverLongitude);

    LatLng requestLocation = new LatLng(requestLatitude, requestLongitude);

    markers.add(mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver's Location")));

    markers.add(mMap.addMarker(new MarkerOptions().position(requestLocation).title("Rider's Location").
            icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

    LatLngBounds.Builder builder = new LatLngBounds.Builder();

    for (Marker marker : markers) {

      builder.include(marker.getPosition());

    }

    LatLngBounds bounds = builder.build();

    int width = getResources().getDisplayMetrics().widthPixels;// RelativeLayout width

    int height = getResources().getDisplayMetrics().heightPixels;// RelativeLayout height

    int padding = (int) (width * 0.22); // offset from edges of the map 12% of screen

    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));

  }
}
