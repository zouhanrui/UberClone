/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.parse.starter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class MainActivity extends AppCompatActivity {

  Button button;

  Switch userModeSwitch;

  public void redirect2Activity() {

    if (ParseUser.getCurrentUser().getString("userMode").matches("Rider")) {

      Intent intent = new Intent(getApplicationContext(), RiderActivity.class);

      startActivity(intent);

    } else if (ParseUser.getCurrentUser().getString("userMode").matches("Driver")) {

      Intent intent = new Intent(getApplicationContext(), requestListActivity.class);

      startActivity(intent);

    }

  }

  public void click(View view) {

    String userMode = "Rider";

    if (userModeSwitch.isChecked()) {

      userMode = "Driver";

    }

    ParseUser.getCurrentUser().put("userMode", userMode);

    ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
      @Override
      public void done(ParseException e) {

        if (e == null) {

          Log.i("info", "UserMode saved");

          redirect2Activity();

        } else {

          Log.i("info", e.getMessage());

        }

      }
    });

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    button = (Button) findViewById(R.id.button);

    userModeSwitch = (Switch) findViewById(R.id.userModeSwitch);

    getSupportActionBar().hide();

    ParseUser.logOut();

    if (ParseUser.getCurrentUser() == null) {

      ParseAnonymousUtils.logIn(new LogInCallback() {

        @Override
        public void done(ParseUser user, ParseException e) {

          if (e == null) {

            Log.i("info", "Anonymous user created and logged in");

            Toast.makeText(MainActivity.this, "Choose MODE", Toast.LENGTH_LONG).show();

          } else {

            Log.i("info", "Anonymous user Logged in failed");

          }

        }
      });
    } else {

      redirect2Activity();

    }

    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}