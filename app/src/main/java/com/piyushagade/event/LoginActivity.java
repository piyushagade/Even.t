package com.piyushagade.event;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class LoginActivity extends Activity{

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private static final String PREF_FILE = "com.piyushagade.event.preferences";
    SharedPreferences sp;
    private SharedPreferences.Editor ed;
    private String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

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
                // ...
            }
        };

        //Read SharedPreferences
        sp = getSharedPreferences(PREF_FILE, 0);
        ed = sp.edit();


        //Login Button Listener
        findViewById(R.id.b_login).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);
                findViewById(R.id.rl_login_section).setVisibility(View.GONE);
                findViewById(R.id.rl_login_wait).setVisibility(View.VISIBLE);
                login();

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                DatabaseReference database = FirebaseDatabase.getInstance().getReference();

                if (user != null) {
                    for (UserInfo profile : user.getProviderData()) {
                        String providerId = profile.getProviderId();

                        String uid = profile.getUid();

                        String name = profile.getDisplayName();
                        String email = profile.getEmail();

                        ed.putString("user_name", name).commit();
                        ed.putString("user_email", email).commit();
                        ed.putString("user_uid", uid).commit();
                        ed.putString("user_device_name", (getDeviceName())).commit();


                    };
                }

                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 2000);

            }
        });

        //Register Button Listener
        findViewById(R.id.b_register).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);
                register();


            }
        });

        //Goto register page Button Listener
        findViewById(R.id.b_register_page).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);
                findViewById(R.id.rl_login_section).setVisibility(View.GONE);
                findViewById(R.id.rl_register_section).setVisibility(View.VISIBLE);
            }
        });

        //Goto login page Button Listener
        findViewById(R.id.b_login_page).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                vibrate(28);
                findViewById(R.id.rl_login_section).setVisibility(View.VISIBLE);
                findViewById(R.id.rl_register_section).setVisibility(View.GONE);

            }
        });


        if(sp.getBoolean("user_logged_in", true)){
            findViewById(R.id.rl_login_section).setVisibility(View.GONE);
            findViewById(R.id.rl_register_section).setVisibility(View.GONE);


            findViewById(R.id.rl_login_wait).setVisibility(View.VISIBLE);

            (new Handler()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    findViewById(R.id.rl_login_wait).setVisibility(View.GONE);
                    finish();
                }
            }, 3000);

        }

    }


    private void login(){
        mAuth.signInWithEmailAndPassword(((EditText)findViewById(R.id.login_email)).getText().toString().toLowerCase(),
                ((EditText)findViewById(R.id.login_password)).getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(((EditText)findViewById(R.id.login_email)).getText().toString().equals("")||
                                ((EditText)findViewById(R.id.login_password)).getText().toString().equals("")){
                            makeToast("Please do not leave any field empty.");
                            vibrate(50);
                        }
                        else if(!isEmail(((EditText)findViewById(R.id.login_email)).getText().toString())) {
                            makeToast("Enter a valid email address.");
                            findViewById(R.id.rl_login_wait).setVisibility(View.GONE);
                        }

                        else if(((EditText)findViewById(R.id.login_password)).getText().toString().length() < 8){
                            makeToast("Password should be at least 8 digits long.");
                            findViewById(R.id.rl_login_wait).setVisibility(View.GONE);
                        }
                        else if (!task.isSuccessful()) {
                            makeToast("Authentication failed. User not found.");
                            vibrate(55);
                            findViewById(R.id.rl_login_wait).setVisibility(View.GONE);
                        }
                        else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            findViewById(R.id.rl_login_wait).setVisibility(View.GONE);
                            ed.putBoolean("user_logged_in", true).commit();


                            ed.putString("user_email", ((EditText)findViewById(R.id.login_email)).getText().toString()).commit();
                            ed.putString("user_password", encrypt(encrypt(((EditText)findViewById(R.id.login_password)).getText().toString()))).commit();
                            ed.putString("user_device_name", (getDeviceName())).commit();

                        }
                    }
                });
    }

    private void register() {
        mAuth.createUserWithEmailAndPassword(((EditText)findViewById(R.id.register_email)).getText().toString().toLowerCase(),
                ((EditText)findViewById(R.id.register_password)).getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(((EditText)findViewById(R.id.register_email)).getText().toString().equals("")||
                                ((EditText)findViewById(R.id.register_password)).getText().toString().equals("")||
                                ((EditText)findViewById(R.id.register_name)).getText().toString().equals("")){
                            makeToast("Please do not leave any field empty.");
                            vibrate(50);
                        }
                        else if(!isEmail(((EditText)findViewById(R.id.register_email)).getText().toString()))
                            makeToast("Email address is invalid.");
                        else if(((EditText)findViewById(R.id.register_password)).getText().toString().length() < 8)
                            makeToast("Password should be at least 8 digits long.");
                        else if (!task.isSuccessful()) {
                            makeToast("User already exists. Please sign in.");
                        }
                        else {
                            findViewById(R.id.rl_login_section).setVisibility(View.VISIBLE);
                            findViewById(R.id.rl_register_section).setVisibility(View.GONE);

                            ((EditText)findViewById(R.id.login_email)).setText(((EditText)findViewById(R.id.register_email)).getText().toString().toLowerCase());
                            ((EditText)findViewById(R.id.login_password)).setText(((EditText)findViewById(R.id.register_password)).getText().toString().toLowerCase());

                            ed.putString("user_email", ((EditText)findViewById(R.id.register_email)).getText().toString()).commit();
                            ed.putString("user_name", ((EditText)findViewById(R.id.register_name)).getText().toString()).commit();
                            ed.putString("user_device_name", (getDeviceName())).commit();

                            //Update profile
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(((EditText)findViewById(R.id.register_name)).getText().toString())
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                makeToast("User account successfully created.");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mAuth!=null) mAuth.addAuthStateListener(mAuthListener);
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

    //Check if Network is available
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private boolean hasPermission()
    {
        String permission = "android.permission.GET_ACCOUNTS";
        int res = getApplicationContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }


    //Encrypt function
    private String encrypt(String data) {
        int k = data.length();
        int m = (k + 1)/2;

        char raw[] = data.toCharArray();
        char temp[] = new char[k];

        for(int j = 0; j < k; j++){
            if(j >= 0 && j < m){
                temp[2*j] = raw[j];
            }
            else if(j >= m  && j <= k - 1){
                if(k % 2 == 0) temp[2*j - k + 1] = raw[j];
                else temp[2*j - k] = raw[j];
            }
        }

        return String.valueOf(temp);
    }

    //Decrypt function
    private String decrypt(String data){
        int k = data.length();
        int m = (k + 1)/2;

        char raw[] = data.toCharArray();
        char temp[] = new char[k];

        for(int j = 0; j < k; j++){
            if(j >= 0 && j < m){
                temp[j] = raw[2*j];
            }
            else if(j >= m  && j <= k - 1){
                if(k % 2 == 0) temp[j] = raw[2*j - k + 1];
                else temp[j] = raw[2*j - k];
            }

        }
        return String.valueOf(temp);
    }

    //Swing animate view
    private void swingAnimate(final View v, final int duration, final int delay) {
        //App title animation
        Handler app_title_anim_handler = new Handler();
        app_title_anim_handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                YoYo.with(Techniques.Swing)
                        .duration(duration)
                        .playOn(v);
            }
        }, delay);
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

    public boolean isEmail(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }
}