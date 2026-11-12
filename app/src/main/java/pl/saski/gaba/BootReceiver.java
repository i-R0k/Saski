package pl.saski.gaba;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Reminders.createChannel(context);
        Reminders.scheduleAll(context);
    }
}
