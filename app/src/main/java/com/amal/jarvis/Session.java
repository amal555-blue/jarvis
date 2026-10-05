package com.amal.jarvis;
import android.app.*;import android.content.*;import android.os.*;
public class Session extends Application {
 public static volatile boolean unlocked=false;
 public void onCreate(){super.onCreate();IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);BroadcastReceiver r=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(WakeService.instance!=null)WakeService.instance.screenLocked();}};if(Build.VERSION.SDK_INT>=33)registerReceiver(r,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(r,f);}
 public static boolean valid(Context c){return unlocked&&!c.getSystemService(KeyguardManager.class).isKeyguardLocked();}
}
