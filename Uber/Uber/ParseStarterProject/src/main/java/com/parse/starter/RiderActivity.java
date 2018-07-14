package com.parse.starter;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

  private GoogleMap mMap;

  LocationManager locationManager;

  LocationListener locationListener;

  Boolean requestActive = false;

  Button uberCallButton;

  TextView infoTextView;

  Handler handler = new Handler();

  ParseGeoPoint driverGeoLocation;

  Boolean isCallingUber = false;

  public void checkForUpdates() {

    if (isCallingUber) {

      final ParseQuery<ParseObject> query = ParseQuery.getQuery("request");

      query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

      query.whereExists("requestAccepted");

      query.findInBackground(new FindCallback<ParseObject>() {
        @Override
        public void done(List<ParseObject> objects, ParseException e) {

          if (e == null && objects.size() > 0) {

            String driverName = objects.get(0).getString("requestAccepted");

            ParseQuery<ParseUser> query1 = ParseUser.getQuery();

            query1.whereEqualTo("username", driverName);

            query1.findInBackground(new FindCallback<ParseUser>() {
              @Override
              public void done(List<ParseUser> objects, ParseException e) {

                if (e == null && objects.size() > 0) {

                  driverGeoLocation = objects.get(0).getParseGeoPoint("location");

                  if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (lastKnownLocation != null) {

                      ParseGeoPoint riderGeoLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                      Double dis = riderGeoLocation.distanceInMilesTo(driverGeoLocation);

                      Double distance = (double) Math.round(dis * 10) / 10;

                      if (distance < 0.001) {

                        infoTextView.setText("Your driver is here");

                        ParseQuery<ParseObject> query2 = ParseQuery.getQuery("request");

                        query2.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

                        query2.findInBackground(new FindCallback<ParseObject>() {
                          @Override
                          public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {

                              for (ParseObject obj : objects) {

                                obj.deleteInBackground();

                              }

                            }
                          }
                        });

                        handler.postDelayed(new Runnable() {
                          @Override
                          public void run() {

                            uberCallButton.setVisibility(View.VISIBLE);

                            uberCallButton.setText("Call Uber");

                            requestActive = false;

                            isCallingUber = false;

                            infoTextView.setText("");
                          }
                        }, 5000);

                      } else {

                        infoTextView.setText("Your driver is " + distance + " miles away");

                        System.out.println("dis: " + distance + "/ driverLoc: " + driverGeoLocation.getLatitude() + "/" + driverGeoLocation.getLongitude());

                        System.out.println("RiderLoc: " + riderGeoLocation.getLongitude() + "/" + riderGeoLocation.getLongitude());

                        infoTextView.setText("Your driver is " + distance + " miles away");

                        LatLng driverLocation = new LatLng(driverGeoLocation.getLatitude(), driverGeoLocation.getLongitude());

                        LatLng requestLocation = new LatLng(riderGeoLocation.getLatitude(), riderGeoLocation.getLongitude());

                        ArrayList<Marker> markers = new ArrayList<>();

                        mMap.clear();

                        markers.add(mMap.addMarker(new MarkerOptions().position(driverLocation).title("Driver's Location")));

                        markers.add(mMap.addMarker(new MarkerOptions().position(requestLocation).title("Your Location").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))));

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();

                        for (Marker marker : markers) {

                          builder.include(marker.getPosition());

                        }

                        LatLngBounds bounds = builder.build();

                        int width = getResources().getDisplayMetrics().widthPixels;

                        int height = getResources().getDisplayMetrics().heightPixels;

                        int padding = (int) (width * 0.22); // offset from edges of the map 12% of screen

                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));

                        handler.postDelayed(new Runnable() {
                          @Override
                          public void run() {

                            Toast.makeText(RiderActivity.this, "Driver is coming ... ", Toast.LENGTH_SHORT).show();

                            checkForUpdates();

                          }
                        }, 5000);

                      }

                    }

                  }

                }
              }
            });
          } else {

            handler.postDelayed(new Runnable() {
              @Override
              public void run() {

                Toast.makeText(RiderActivity.this, "Searching for driver ... ", Toast.LENGTH_SHORT).show();

                checkForUpdates();

              }
            }, 5000);

          }
        }
      });
    }

  }

  public void logOut(View view) {

    ParseUser.logOut();

    isCallingUber = false;

    Intent intent = new Intent(this, MainActivity.class);

    startActivity(intent);

  }

  public void uberCall(View view) {

    if (requestActive) {

      ParseQuery<ParseObject> query = new ParseQuery<>("request");

      query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

      query.findInBackground(new FindCallback<ParseObject>() {
        @Override
        public void done(List<ParseObject> objects, ParseException e) {

          if (e == null) {

            if (objects.size() > 0) {

              for (ParseObject obj : objects) {

                obj.deleteInBackground(new DeleteCallback() {

                  @Override
                  public void done(ParseException e) {

                    if (e == null) {

                      Toast.makeText(RiderActivity.this, "Request Cancelled!", Toast.LENGTH_SHORT).show();

                    }

                  }
                });

              }

              isCallingUber = false;

              requestActive = false;

              uberCallButton.setText("Call Uber");

            }
          }
        }
      });

    } else {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {

          ParseObject request = new ParseObject("request");

          ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

          request.put("username", ParseUser.getCurrentUser().getUsername());

          request.put("location", parseGeoPoint);

          request.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {

              if (e == null) {

                isCallingUber = true;

                requestActive = true;

                uberCallButton.setText("Cancel Uber");

                Toast.makeText(RiderActivity.this, "Request Sent!", Toast.LENGTH_SHORT).show();

                checkForUpdates();

                Toast.makeText(RiderActivity.this, "checkForUpdate!", Toast.LENGTH_SHORT).show();

              } else {

                Log.i("Info", "save failed");

              }

            }
          });
        } else {

          Toast.makeText(this, "Last Known Location is NOT found", Toast.LENGTH_SHORT).show();

        }
      }

    }



  }

  public void permissionCheck() {

    if (Build.VERSION.SDK_INT < 23) {

      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    } else {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

      } else {

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {

          markerUpdate(lastKnownLocation);

        }

      }

    }

  }

  public void markerUpdate(Location location) {

    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

    mMap.clear();

    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

    mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == 1 && permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

      permissionCheck();

    }

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_rider);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    uberCallButton = (Button) findViewById(R.id.uberCallButton);

    infoTextView = (TextView) findViewById(R.id.infoTextView);

    ParseQuery<ParseObject> query = new ParseQuery<>("request");

    query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

    query.findInBackground(new FindCallback<ParseObject>() {
      @Override
      public void done(List<ParseObject> objects, ParseException e) {

        if (e == null) {

          if (objects.size() > 0) {

            requestActive = true;

            uberCallButton.setText("Cancel Uber");

            checkForUpdates();

          }

        }

      }
    });

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

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {

        markerUpdate(location);

      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle extras) {

      }

      @Override
      public void onProviderEnabled(String provider) {

      }

      @Override
      public void onProviderDisabled(String provider) {

      }
    };

    permissionCheck();


  }
}


/*


 */