package org.monero.monero_wallet_gui;

import org.monero.monero_wallet_gui.R;
import org.qtproject.qt5.android.bindings.QtService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.support.v4.app.NotificationCompat;
import android.content.res.Resources;

import android.util.Log;
import android.os.IBinder;
import android.os.Handler;
import android.content.IntentFilter;

import java.util.Properties;

import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.i2ptunnel.I2PTunnel;
import net.i2p.i2ptunnel.TunnelControllerGroup;


public class MoneroI2PService extends QtService
{
    private static final String TAG = "I2PService";
    private static NotificationManager m_notificationManager;
    private static Notification.Builder m_builder;
    private static String CHANNEL_ID = "monero_channel_i2p";
    private RouterContext _context;
    private int mInterval = 5000;
    private Handler mHandler;

    public MoneroI2PService() {}

    private Notification createNotification(String text, String bigtext) {
            m_notificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "monero_channel_i2p",
                    NotificationManager.IMPORTANCE_LOW);
            m_notificationManager.createNotificationChannel(channel);
     
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.itoopie_sm)
                    .setContentTitle("TODO")
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(bigtext))
                    .build();
            return notification;
    }

    public void update() {
        RouterContext ctx = _context;
        if (ctx == null) {
            Log.i(TAG, "null context");
            return;
        }

        int active = ctx.commSystem().countActivePeers();
        int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
        int inEx = ctx.tunnelManager().getFreeTunnelCount();
        int outEx = ctx.tunnelManager().getOutboundTunnelCount();
        int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
        int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
        double inBW = ctx.bandwidthLimiter().getReceiveBps();
        double outBW = ctx.bandwidthLimiter().getSendBps();

        String text =
                getResources().getString(R.string.notification_status_text,
                        I2PHelpers.formatSpeed(inBW), I2PHelpers.formatSpeed(outBW));

        String bigText =
                getResources().getString(R.string.notification_status_bw,
                        I2PHelpers.formatSpeed(inBW), I2PHelpers.formatSpeed(outBW)) + '\n'
                + getResources().getString(R.string.notification_status_peers,
                        active, known) + '\n'
                + getResources().getString(R.string.notification_status_expl,
                        inEx, outEx) + '\n'
                + getResources().getString(R.string.notification_status_client,
                        inCl, outCl);

        m_notificationManager.notify(1, createNotification(text, bigText));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Creating Service");
        mHandler = new Handler();
        mStatusChecker.run();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, createNotification("Monero I2P Service", "Initializing ..."));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "Starting I2P");
        new Thread(new Starter()).start();
        return ret;
    }

    private class Starter implements Runnable {
        private void setupConfigDir(Context ctx, String baseDir) {
            I2PHelpers.copyResourceToFile(ctx, R.raw.tunnel_config, baseDir, "i2ptunnel.config");
        }
        private void setupBaseDir(Context ctx, String baseDir) {
            I2PHelpers.unzipResourceToDir(ctx, R.raw.certificates_zip, baseDir, "certificates");
            I2PHelpers.copyResourceToFile(ctx, R.raw.blocklist_txt, baseDir, "blocklist.txt");
            I2PHelpers.copyResourceToFile(ctx, R.raw.router_config, baseDir, "router.config");
            I2PHelpers.copyResourceToFile(ctx, R.raw.logger_config, baseDir, "logger.config");
        }
        public void run() {
            // See https://geti2p.net/en/docs/applications/embedding
            Log.i(TAG, "I2P run starting");
            Context ctx = getApplicationContext();
            String baseDir = ctx.getFilesDir().getAbsolutePath();
            setupBaseDir(ctx, baseDir + "/i2p.base");
            setupConfigDir(ctx, baseDir + "/i2p.config");
            Properties p = new Properties();
            p.setProperty("i2p.dir.base", baseDir + "/i2p.base");
            p.setProperty("i2p.dir.config", baseDir + "/i2p.config");
            p.setProperty("wrapper.logfile", baseDir + "/i2p.config/wrapper.log");
            p.setProperty("i2np.inboundKBytesPerSecond", "50");
            p.setProperty("i2np.outboundKBytesPerSecond", "50");
            p.setProperty("router.sharePercentage", "80");
            p.setProperty("i2np.upnp.enable", Boolean.toString(false));
            Router r = new Router(p);
            r.setKillVMOnEnd(false);
            r.runRouter();
            _context = r.getContext();
            while(!r.isRunning()){
                if(!r.isAlive()) {
                    Log.e(TAG, "Not even alive");
                }
                Log.i(TAG, "Not running yet, will sleep 1s");
                try {
                    Thread.sleep(1000);
                } catch(Exception e) {
                    Log.e(TAG, "exception during sleep");
                }
            }
            TunnelControllerGroup tcg = TunnelControllerGroup.getInstance(r.getContext());
            try {
                tcg.startup();
                int sz = tcg.getControllers().size();
                Log.i(TAG, "i2ptunnel started " + sz + " clients");
            } catch (IllegalArgumentException iae) {
                Log.e(TAG, "i2ptunnel failed to start", iae);
            }
        }
    }

    Runnable mStatusChecker = new Runnable() {
        @Override 
        public void run() {
              try {
                   update();
              } finally {
                   mHandler.postDelayed(mStatusChecker, mInterval);
              }
        }
    };
}
