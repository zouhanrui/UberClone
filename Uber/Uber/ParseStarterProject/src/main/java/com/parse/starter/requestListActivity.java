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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.cast.framework.media.MediaNotificationService;
import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class requestListActivity extends AppCompatActivity {

  ArrayList<String> requestList;

  ArrayList<Double> ridersLatitude;

  ArrayList<Double> ridersLongitude;

  ArrayList<String> ridersUsername;

  ArrayAdapter adapter;

  ListView requestListView;

  LocationManager locationManager;

  LocationListener locationListener;

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == 1 && permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {

          updateRequestList(lastKnownLocation);

        } else {

          Toast.makeText(this, "Driver's Location is not FOUND!", Toast.LENGTH_SHORT).show();

        }

      } else {

        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);

      }

    }

  }

  public void updateRequestList(Location location) {

    if (location != null) {

      final ParseGeoPoint driverCurrentGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());

      ParseQuery<ParseObject> query = ParseQuery.getQuery("request");

      query.whereNear("location", driverCurrentGeoPoint);

      query.whereDoesNotExist("requestAccepted");

      query.setLimit(10);

      query.findInBackground(new FindCallback<ParseObject>() {
        @Override
        public void done(List<ParseObject> objects, ParseException e) {

          if (e == null) {

            requestList.clear();

            ridersLongitude.clear();

            ridersLatitude.clear();

            ridersUsername.clear();

            if (objects.size() > 0) {

              for (ParseObject obj : objects) {

                ParseGeoPoint riderCurrentGeoPoint = obj.getParseGeoPoint("location");

                Double distance = driverCurrentGeoPoint.distanceInMilesTo(riderCurrentGeoPoint);

                Double distanceOneDP = (double) Math.round(distance * 10) / 10;

                requestList.add(distanceOneDP + " miles");

                ridersLatitude.add(riderCurrentGeoPoint.getLatitude());

                ridersLongitude.add(riderCurrentGeoPoint.getLongitude());

                ridersUsername.add(obj.getString("username"));

              }

            } else {

              Toast.makeText(requestListActivity.this, "No active request nearby", Toast.LENGTH_SHORT).show();

            }

            adapter.notifyDataSetChanged();

          } else {

            Log.i("Info", e.getMessage());

          }

        }
      });
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    MenuInflater menuInflater = getMenuInflater();

    menuInflater.inflate(R.menu.main_menu, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {

      case R.id.logOut:

        ParseUser.logOut();

        Intent intent = new Intent(this, MainActivity.class);

        startActivity(intent);

        break;

    }

    return super.onOptionsItemSelected(item);

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_request_list);

    requestListView = (ListView) findViewById(R.id.requestListView);

    setTitle("Nearby Request");

    requestList = new ArrayList<>();

    ridersLatitude = new ArrayList<>();

    ridersLongitude = new ArrayList<>();

    ridersUsername = new ArrayList<>();

    adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requestList);

    requestListView.setAdapter(adapter);

    requestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

          Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

          if (ridersLatitude.size() > position && ridersUsername.size() > position && ridersLongitude.size() > position && lastKnownLocation != null) {

            Intent intent = new Intent(getApplicationContext(), DriverViewActivity.class);

            intent.putExtra("driverLatitude", lastKnownLocation.getLatitude());

            intent.putExtra("driverLongitude", lastKnownLocation.getLongitude());

            intent.putExtra("requestLatitude", ridersLatitude.get(position));

            intent.putExtra("requestLongitude", ridersLongitude.get(position));

            intent.putExtra("username", ridersUsername.get(position));

            startActivity(intent);
          }
        }
      }
    });

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    locationListener = new LocationListener() {
      @Override
      public void onLocationChanged(Location location) {

        updateRequestList(location);

        ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(), location.getLongitude()));

        ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
          @Override
          public void done(ParseException e) {

            if (e == null) {

              Log.i("Info", "Driver location updated and saved");

            }

          }
        });
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

    if (Build.VERSION.SDK_INT < 23) {

      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    } else {

      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

      } else {

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastKnownLocation != null) {

          updateRequestList(lastKnownLocation);

        } else {

          Toast.makeText(this, "Driver's Location is not FOUND!", Toast.LENGTH_SHORT).show();

        }

      }

    }



  }
}
