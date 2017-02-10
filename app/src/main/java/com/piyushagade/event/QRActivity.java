package com.piyushagade.event;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class QRActivity extends Activity implements QRCodeReaderView.OnQRCodeReadListener {

    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();

    private static final String PREF_FILE = "com.piyushagade.event.preferences";
    private TextView myTextView;
    private QRCodeReaderView mydecoderview;

    private DatabaseReference fb, fb_desktops;
    private String sp_user_email;
    SharedPreferences sp;
    private SharedPreferences.Editor ed;
    private String user_node, user_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_decoder);



        //Hint
        makeSnack("Scan QR code on your desktop");

        //Read SharedPreferences
        sp = getSharedPreferences(PREF_FILE, 0);
        ed = sp.edit();
        sp_user_email = sp.getString("user_email", "unknown");


        //Format email address (remove the .)
        user_node = encrypt(encrypt(encrypt(sp_user_email.replaceAll("\\.", ""))));

        //Firebase
        fb = mRootRef.child("cloudboard").child(user_node);

        user_email = encrypt(encrypt(encrypt(sp_user_email.replaceAll("\\.", ""))));

        //Signal desktop application to come to front
        fb.child("reauthorization").setValue("2");

        //DecoderView
        mydecoderview = (QRCodeReaderView) findViewById(R.id.qrdecoderview);
        mydecoderview.setOnQRCodeReadListener(this);

        //OnCLick Listeners
        //Close Button listener
        (findViewById(R.id.b_qr_back)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();

            }

        });

        //Help Button listener
        (findViewById(R.id.b_qr_help)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();

            }

        });
    }

    //k ensures that pairing occurs only once
    int k = 0;

    @Override
    public void onQRCodeRead(String decoded_text, PointF[] points) {
        if(isNetworkAvailable())

            //If decoded text is a valid UniClip device ID
            if(!decoded_text.equals("") && decoded_text.length() == 18){
                if(k == 0) {
                    k = 1;


                    //Reset reauthorization state
                    fb.child("reauthorization").setValue("0");

                    //Firebase Object
                    DatabaseReference fb_desktops = mRootRef.child("desktops");



                    //Associate the desktop with this account
                    fb_desktops.child(decoded_text).setValue(String.valueOf(user_email));

                    if(isNetworkAvailable()) makeSnack("Pairing successful.");
                    else  makeSnack("Pairing successful. Waiting for network.");

                    vibrate(100);

                    //Delay  tear down of the activity
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 1800);

                }
            }

            else if(!decoded_text.equals("") && decoded_text.length() != 18){
                makeSnack("Invalid QR code");
            }

            else {
                makeSnack("Internet not available.");
            }

    }


    // Called when your device have no camera
    @Override
    public void cameraNotFound() {
        makeSnack("Cannot initiate camera.");
    }

    // Called when there's no QR codes in the camera preview image
    @Override
    public void QRCodeNotFoundOnCamImage() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mydecoderview != null) mydecoderview.getCameraManager().startPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mydecoderview != null) mydecoderview.getCameraManager().stopPreview();
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

    //Make Snack
    public void makeSnack(String t){
        View v = findViewById(R.id.rl_decoder);
        Snackbar.make(v, t, Snackbar.LENGTH_LONG).setAction("Action", null).show();
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
}