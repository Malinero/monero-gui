package org.monero.monero_wallet_gui;

import org.monero.monero_wallet_gui.R;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import net.freehaven.tor.control.RawEventListener;
import net.freehaven.tor.control.TorControlCommands;
import net.freehaven.tor.control.TorControlConnection;
import org.torproject.jni.TorService;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class MoneroTorService extends TorService {

    private static NotificationManager m_notificationManager;
    private static String CHANNEL_ID = "monero_channel_tor";
    private static String mStatus = TorService.STATUS_OFF;
    private TorControlConnection m_torControlConnection;
    private long mWritten, mRead, mTotalBandwidthWritten, mTotalBandwidthRead;
    public static String ACTION_STOP = "org.monero.monero_wallet_gui.STOP";
    BroadcastReceiver mStatusReceiver;
    private TorEventListener mListener;

    private Notification createNotification() {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "monero_channel_tor",
                    NotificationManager.IMPORTANCE_LOW);
            m_notificationManager.createNotificationChannel(channel);

            String text = getResources().getString(R.string.notification_status_text,
                        formatBandwidthCount(mRead), formatBandwidthCount(mWritten));

            String bigtext = "Status : " + mStatus + "\nTotal: " + getResources().getString(R.string.notification_status_text, formatBandwidthCount(mTotalBandwidthRead), formatBandwidthCount(mTotalBandwidthWritten));;

            Intent stopSelf = new Intent(this, MoneroTorService.class);
            stopSelf.setAction(this.ACTION_STOP);
            PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.toron)
                    .setContentTitle("Monero Tor Service (" + mStatus + ")")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(bigtext))
                    .addAction(R.drawable.toroff, "Stop", pStopSelf)
                    .build();
            return notification;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        m_notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));

        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mStatus = intent.getStringExtra(TorService.EXTRA_STATUS);
                m_notificationManager.notify(1, createNotification());
            }
        };
        registerReceiver(mStatusReceiver, new IntentFilter(TorService.ACTION_STATUS));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, createNotification());
        }
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        Log.e(TAG, "Received intent");
        if (!TextUtils.isEmpty(action) && action.equals(this.ACTION_STOP)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            clearNotifications();
            stopSelf();
            return START_NOT_STICKY;
        }
        while ((m_torControlConnection=this.getTorControlConnection()) ==null)
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (m_torControlConnection == null) {
            Log.e(TAG, "Failed to get a tor control connection");
            return START_NOT_STICKY;
        }

        try {
            mListener = new TorEventListener(MoneroTorService.this);
            m_torControlConnection.addRawEventListener(mListener);
            ArrayList<String> events = new ArrayList<>(Arrays.asList(TorControlCommands.EVENT_CIRCUIT_STATUS, TorControlCommands.EVENT_BANDWIDTH_USED));
            m_torControlConnection.setEvents(events);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return START_NOT_STICKY;
    }

    private void clearNotifications() {
        if (m_notificationManager != null)
            m_notificationManager.cancelAll();
    }

@Override
public void onDestroy()
{
    Log.e(TAG, "onDestroy");
        if (m_torControlConnection != null) {
            m_torControlConnection.removeRawEventListener(mListener);
        }

    unregisterReceiver(mStatusReceiver);
    super.onDestroy();
}

    public void bandwidthChanged(long read, long written) {
        mWritten = written;
        mRead = read;
        mTotalBandwidthWritten += written;
        mTotalBandwidthRead += read;
        m_notificationManager.notify(1, createNotification());
    }

    public String formatBandwidthCount(long bitsPerSecond) {
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        if (bitsPerSecond < 1e6)
            return nf.format(Math.round(((float) ((int) (bitsPerSecond * 10 / 1024)) / 10))) + "KiB/s";
        return nf.format(Math.round(((float) ((int) (bitsPerSecond * 100 / 1024 / 1024)) / 100))) + "MiB/s";
    }
 

    public class TorEventListener implements RawEventListener {
        private final MoneroTorService mService;
    
        TorEventListener(MoneroTorService service) {
            mService = service;
        }

        @Override
        public void onEvent(String keyword, String data) {
            if (TorControlCommands.EVENT_BANDWIDTH_USED.equals(keyword)) {
                String[] payload = data.split(" ");
                handleBandwidth(Long.parseLong(payload[0]), Long.parseLong(payload[1]));
            } else {
                Log.e(TAG, "Unexpected (" + keyword + "): " + data);
            }
        }
        private void handleBandwidth(long read, long written) {
            mService.bandwidthChanged(read, written);
        }
   
    }
}
