package com.dsblue.rndisconfig.rndis;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * This service watches for RNDIS devices that may connect to the Android device.  There is no mention of the devices
 * in lsusb or the Android UsbManager interface.  To see if a device is attached, this service will ...
 */
public class WatchForDevices extends Service {
    private NotificationManager mNM;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.notification_title;

    private NetworkManager networkManager;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        WatchForDevices getService() {
            return WatchForDevices.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        networkManager = new NetworkManager(getApplicationContext());

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);

        // Check to see if the user wants to start RNDIS mode at start up, if so then enable it
        final SharedPreferences myPrefs = this.getSharedPreferences(getString(R.string.prefrence_file_name), MODE_PRIVATE);

        boolean startRndis = myPrefs.getBoolean(getString(R.string.pref_on_startup), false);

        if (startRndis) {
            String ipString = myPrefs.getString(getString(R.string.pref_ip), getString(R.string.default_ip));
            String maskString = myPrefs.getString(getString(R.string.pref_ip_mask), getString(R.string.default_mask));

            networkManager.startRNDIS(ipString, maskString);
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, RNDIS.class), 0);

        final CharSequence status;
        if (networkManager.isRNDISMode())
            status = getText(R.string.notification_details_rndis);
        else
            status = getText(R.string.notification_details_no_rndis);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentIntent(contentIntent)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(status)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    public void enterRNDISMode(String ip, String mask){
        networkManager.startRNDIS(ip, mask);
        showNotification();
    }

    public void exitRNDISMode(){
        networkManager.stopRNDIS();
        showNotification();
    }

    public void probeDHCP(){
        networkManager.probeDHCP();
    }

    public boolean isInRNDISMode() {
        return networkManager.isRNDISMode();
    }
}
