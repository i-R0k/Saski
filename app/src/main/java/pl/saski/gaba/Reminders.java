package pl.saski.gaba;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.Calendar;

public class Reminders extends BroadcastReceiver {
    static final String CHANNEL = "gaba_medications";
    static final int BASE_REQUEST = 500;
    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("gaba_local_data",Context.MODE_PRIVATE);
    }
    static void createChannel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        NotificationChannel ch=new NotificationChannel(CHANNEL,"Leki Gaby",NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Przypomnienia o lekach i kontroli weterynaryjnej");
        nm.createNotificationChannel(ch);
    }
    static PendingIntent alarmIntent(Context context,int slot) {
        Intent intent=new Intent(context,Reminders.class);
        intent.putExtra("slot",slot);
        return PendingIntent.getBroadcast(context,BASE_REQUEST+slot,intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean anyActive(SharedPreferences p,int slot) {
        for(MainActivity.Med med: MainActivity.MEDS)
            if(med.hasSlot(slot)&&MainActivity.active(p,med))return true;
        return false;
    }
    static void scheduleAll(Context context) {
        SharedPreferences p=preferences(context);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        for(int slot=0;slot<5;slot++) {
            PendingIntent pi=alarmIntent(context,slot);
            am.cancel(pi);
            if(p.getBoolean("reminders_enabled",true)&&anyActive(p,slot))scheduleSlot(context,slot);
        }
        for(int i=0;i<2;i++) {
            PendingIntent pi=alarmIntent(context,5+i);am.cancel(pi);
            if(p.getBoolean("reminders_enabled",true))scheduleControl(context,i);
        }
    }
    static void scheduleSlot(Context context,int slot) {
        SharedPreferences p=preferences(context);
        if(!p.getBoolean("reminders_enabled",true)||!anyActive(p,slot))return;
        try {
            String[] hm=MainActivity.time(p,slot).split(":");
            Calendar target=Calendar.getInstance();
            target.set(Calendar.HOUR_OF_DAY,Integer.parseInt(hm[0]));
            target.set(Calendar.MINUTE,Integer.parseInt(hm[1]));
            target.set(Calendar.SECOND,0);target.set(Calendar.MILLISECOND,0);
            if(target.getTimeInMillis()<=System.currentTimeMillis())target.add(Calendar.DAY_OF_MONTH,1);
            while(!MainActivity.relevant(MainActivity.date(target),slot))target.add(Calendar.DAY_OF_MONTH,1);
            schedule(context,slot,target.getTimeInMillis());
        }catch(Exception ignored){}
    }
    static void scheduleControl(Context context,int dayIndex) {
        Calendar cal=Calendar.getInstance();cal.set(2026,Calendar.NOVEMBER,15+dayIndex,9,0,0);cal.set(Calendar.MILLISECOND,0);
        if(cal.getTimeInMillis()>System.currentTimeMillis())schedule(context,5+dayIndex,cal.getTimeInMillis());
    }
    static void schedule(Context context,int slot,long at) {
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        PendingIntent pi=alarmIntent(context,slot);
        try {
            if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }catch(SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }
    }
    @Override public void onReceive(Context context,Intent intent) {
        createChannel(context);
        int slot=intent.getIntExtra("slot",-1);
        if(slot<0||slot>6)return;
        if(slot<5) {
            scheduleSlot(context,slot);
            SharedPreferences p=preferences(context);
            if(!p.getBoolean("reminders_enabled",true))return;
            String day=MainActivity.today();
            if(!MainActivity.relevant(day,slot))return;
            ArrayList<String> pending=new ArrayList<>();
            for(MainActivity.Med m:MainActivity.MEDS)
                if(m.hasSlot(slot)&&MainActivity.active(p,m)&&!MainActivity.done(p,day,m,slot))
                    pending.add(m.name+" "+m.dose);
            if(pending.isEmpty())return;
            notify(context,slot,"Czas podać leki Gabie",android.text.TextUtils.join(" · ",pending));
        } else {
            if(preferences(context).getBoolean("reminders_enabled",true))
                notify(context,slot,"Kontrola weterynaryjna Gaby","Kontrola zalecana 15–16 listopada 2026 r. Przy pogorszeniu — natychmiast.");
        }
    }
    static void notify(Context context,int slot,String title,String content) {
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)return;
        Intent i=new Intent(context,MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent click=PendingIntent.getActivity(context,slot+700,i,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(context,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(content)
            .setStyle(new Notification.BigTextStyle().bigText(content))
            .setContentIntent(click).setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER).build();
        nm.notify(100+slot,n);
    }
}
