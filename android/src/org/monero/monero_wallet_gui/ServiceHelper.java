package org.monero.monero_wallet_gui;

import android.content.Context;
import android.content.Intent;

import org.monero.monero_wallet_gui.MoneroTorService;
import org.monero.monero_wallet_gui.MoneroI2PService;

public class ServiceHelper {
    
    public static void startTor(Context context) {
        Intent intent = new Intent(context, MoneroTorService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopTor(Context context) {
        Intent intent = new Intent(context, MoneroTorService.class);
        context.stopService(intent);
    }

    public static void startI2P(Context context) {
        Intent intent = new Intent(context, MoneroI2PService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopI2P(Context context) {
        Intent intent = new Intent(context, MoneroI2PService.class);
        context.stopService(intent);
    }

}
