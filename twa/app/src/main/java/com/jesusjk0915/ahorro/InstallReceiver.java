package com.jesusjk0915.ahorro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receiver declarado en el Manifest: sigue vivo aunque la app esté en segundo plano. */
public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Installer.handle(context, intent);
    }
}
