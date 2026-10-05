package com.amal.jarvis;
import android.app.*;import android.content.*;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import android.graphics.Color;
public class BubbleService extends Service {
 WindowManager manager; TextView bubble;
 public IBinder onBind(Intent intent){return null;}
 @Override public void onCreate(){super.onCreate();
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("bubble","JARVIS bubble",NotificationManager.IMPORTANCE_LOW));
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,BubbleService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
  startForeground(1,new Notification.Builder(this,"bubble").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS bubble active").setContentText("Tap J to open. Microphone is off.").addAction(new Notification.Action.Builder(null,"Stop",stop).build()).build());
  manager=getSystemService(WindowManager.class);bubble=new TextView(this);bubble.setText(" J ");bubble.setTextSize(24);bubble.setTextColor(Color.CYAN);bubble.setBackgroundColor(Color.rgb(5,12,20));bubble.setPadding(12,12,12,12);
  WindowManager.LayoutParams params=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);params.gravity=Gravity.TOP|Gravity.END;params.y=200;
  bubble.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP)));
  bubble.setOnLongClickListener(v->{stopSelf();return true;});
  try{manager.addView(bubble,params);}catch(Exception e){stopSelf();}
 }
 public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&"STOP".equals(intent.getAction()))stopSelf();return START_NOT_STICKY;}
 public void onDestroy(){if(manager!=null&&bubble!=null&&bubble.isAttachedToWindow())manager.removeView(bubble);super.onDestroy();}
}
