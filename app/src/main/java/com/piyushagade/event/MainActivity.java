package com.piyushagade.event;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends Activity {
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private final String TAG = "log_ma";
    private View mDecorView;
    private static final String PREF_FILE = "com.piyushagade.event.preferences";
    SharedPreferences sp;
    private SharedPreferences.Editor ed;
    private Intent intent;
    private Boolean add_popup_open = true;
    private DatabaseReference fb_devices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);


        mAuth = FirebaseAuth.getInstance();

        SharedPreferences pref = getSharedPreferences(PREF_FILE, 0);

        //Read SharedPreferences
        sp = getSharedPreferences(PREF_FILE, 0);
        ed = sp.edit();

        //Get Intent
        intent = getIntent();

        // Firebase
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();



        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();

                String uid = profile.getUid();

                String name = profile.getDisplayName();
                String email = profile.getEmail();

                ed.putString("user_name", name).commit();
                ed.putString("user_email", email).commit();
                if(!uid.contains("@"))
                    ed.putString("user_uid", uid).commit();
                ed.putString("user_device_name", (getDeviceName())).commit();


            };

            fb_devices = database.child("users").child(sp.getString("user_uid", "UNKNOWN")).child("profile").child("devices")
                    .child(removeDot(sp.getString("user_device_name", "unknown_device"))).getRef();

            fb_devices.setValue("1");

            Intent intent = new Intent(MainActivity.this, EventService.class);
            startService(intent);
        }

        ((TextView) findViewById(R.id.tv_welcome)).append(sp.getString("user_name", "user."));




        //Add device button listener
        findViewById(R.id.b_add_device).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);
                add_popup_open = !add_popup_open;

                if(add_popup_open){
                    findViewById(R.id.rl_add_popup).setVisibility(View.GONE);
                    ((ImageView) findViewById(R.id.b_add_device)).setImageResource(R.drawable.fab_add);
                }
                else {
                    findViewById(R.id.rl_add_popup).setVisibility(View.VISIBLE);
                    ((ImageView) findViewById(R.id.b_add_device)).setImageResource(R.drawable.fab_back);
                }
            }
        });

        //Logout button listener
        findViewById(R.id.b_logout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);

                ed.putString("user_email", "").commit();
                ed.putString("user_name", "").commit();
                ed.putBoolean("user_logged_in", false).commit();

                DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                fb_devices = database.child("users").child(sp.getString("user_uid", "UNKNOWN")).child("profile").child("devices")
                        .child(removeDot(sp.getString("user_device_name", "unknown_device"))).getRef();

                fb_devices.setValue("0");

                mAuth.signOut();

                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();

            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    //Vibrate method
    private void vibrate(int time){
        ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(time);
    }

    //Make Toast
    private void makeToast(Object data) {
        Toast.makeText(getApplicationContext(),String.valueOf(data),Toast.LENGTH_LONG).show();
    }

    //Get System Device Name
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }

    //Check if a number is even
    private boolean isEven(int i) {
        if(i % 2 == 0) return true;
        return false;
    }

    public String removeDot(String text){
        return text.replaceAll("\\.", "");
    }
}
