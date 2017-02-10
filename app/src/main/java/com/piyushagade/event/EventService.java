package com.piyushagade.event;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EventService extends Service {


    private static final String PREF_FILE = "com.piyushagade.event.preferences";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private static final String TAG = "log_es";
    SharedPreferences sp;
    private SharedPreferences.Editor ed;

    private DatabaseReference fb_user, fb_calls, fb_notifications, fb_texts, fb_music, fb_system, fb_devices, fb_incoming, fb_outgoing;


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Read SharedPreferences
        sp = getSharedPreferences(PREF_FILE, 0);
        ed = sp.edit();

        Log.d(TAG, "Service commenced");


        // Firebase
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        fb_user = database.child("users").child(sp.getString("user_uid", "UNKNOWN")).getRef();
        fb_calls = fb_user.child("calls").getRef();
        fb_devices = fb_user.child(removeDot(sp.getString("user_device_name", "unknown_device"))).getRef();
        fb_music = fb_user.child("music").getRef();
        fb_system = fb_user.child("system").getRef();


        //Calls
        fb_incoming = fb_calls.child("fb_incoming").getRef();
        fb_outgoing = fb_calls.child("fb_outgoing").getRef();


        fb_outgoing.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    String caller = snapshot.getValue().toString();



                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });




        return START_STICKY;


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



    public String removeDot(String text){
        return text.replaceAll("\\.", "");
    }


    //Display Notification
    protected void displayNotification(){
        if(sp.getBoolean("notification", true)) {
            Intent intent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(""));
            PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

            String notification_text = "";
            if(sp.getInt("get_shakes", 0) != 0) notification_text = "Shake your device " + sp.getInt("get_shakes", 0) + " times to copy the incoming data to your clipboard.";
            else notification_text = "Clipboard updated.";

            Notification myNotification = new Notification.Builder(this)
                    .setContentTitle("UniClip!")
                    .setContentText(notification_text)
                    .setTicker("Incoming Data!")
                    .setStyle(new Notification.BigTextStyle().bigText(notification_text))
                    .setLights(Color.WHITE, 200, 100)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pIntent)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.notif_ico)
                    .build();

            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, myNotification);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationManager.cancel(1);
                }
            }, 10000);
        }
        else if(sp.getBoolean("notification", true)) {
            Intent intent = new Intent(Intent.ACTION_DEFAULT, Uri.parse(""));
            PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

            String notification_text = "";
            if(sp.getInt("share_shakes", 2) != 0) notification_text = "Shake your device " + sp.getInt("share_shakes", 2) + " times to open share Clipboard dialog.";


            Notification myNotification = new Notification.Builder(this)
                    .setContentTitle("UniClip!")
                    .setContentText(notification_text)
                    .setTicker("Share Clipboard!")
                    .setStyle(new Notification.BigTextStyle().bigText(notification_text))
                    .setLights(Color.WHITE, 200, 100)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pIntent)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .setSmallIcon(R.drawable.notif_ico)
                    .build();

            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, myNotification);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationManager.cancel(1);
                }
            }, 10000);
        }
    }




    //Display OTA Notification
    protected void displayOTANotification(String text){

        Intent intent;
        PendingIntent pIntent;

        intent = new Intent(EventService.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        String notification_text = text;

        Notification myNotification = new Notification.Builder(this)
                .setContentTitle("UniClip notice!")
                .setContentText(notification_text)
                .setTicker("A notification from developer!")
                .setStyle(new Notification.BigTextStyle().bigText(notification_text))
                .setLights(Color.WHITE, 200, 100)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.notif_ico)
                .build();

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, myNotification);

    }



    //Display Notification for special clips
    protected void displaySpecialNotification(){

        Intent intent;
        PendingIntent pIntent = null;

        if (ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            makeToast("Please grant \"Camera\" permissions in the Even.t application.");
            intent = new Intent(EventService.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        }
        else{


            if(isNetworkAvailable()) {
                intent = new Intent(EventService.this, QRActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
            }
            else
                makeToast("Internet not available.");
        }



        String notification_text = "Click here to authorize your desktop.";

        Notification myNotification = new Notification.Builder(this)
                .setContentTitle("UniClip!")
                .setContentText(notification_text)
                .setTicker("Authorization required!")
                .setStyle(new Notification.BigTextStyle().bigText(notification_text))
                .setLights(Color.WHITE, 200, 100)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.notif_lock_ico)
                .build();

        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, myNotification);


    }



    //Start Browser method
    private void startBrowser(String url){

            if (!url.startsWith("http://") && !url.startsWith("https://"))
                url = "http://" + url;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

    }

    //Start Dialer
    private void startDialer(String number){

            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("tel:" + number));
            startActivity(intent);

    }

    //Check if URL
    private boolean isURL(String url) {
        final String URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";

        Pattern p = Pattern.compile(URL_REGEX);
        Matcher m = p.matcher(url);
        if(m.find()) {
            return true;
        }
        return false;
    }


    //Check if Phone number
    private boolean isPhone(String phoneNumber) {
        System.out.println(phoneNumber.length());
        String regex = "^\\+?[0-9. ()-]{10,25}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phoneNumber);


        if (matcher.matches()) {
            return true;
        } else {
            return false;
        }
    }



    //On Destroy
    @Override
    public void onDestroy() {
        stopSelf();
        super.onDestroy();

    }

    //Make toast method
    private void makeToast(Object data) {
        Toast.makeText(getApplicationContext(),String.valueOf(data),Toast.LENGTH_LONG).show();
    }

    //Network Detector method
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Vibrate device method
    private void vibrate(int time){
        if(sp.getBoolean("vibrate", true))((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(time);
    }



}