package com.amal.jarvis;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import android.speech.tts.*;
import org.vosk.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** User-started microphone service. No boot restart and no voice identity claim. */
public class WakeService extends Service {
 public static volatile WakeService instance;
 public static boolean pendingWake=false;
 final Handler ui=new Handler(Looper.getMainLooper());
 volatile boolean running=true, paused=false, command=false;
 volatile int generation=0;
 Thread listener; Model model; TextToSpeech tts; boolean ttsReady;
 PowerManager.WakeLock power; WindowManager wm; LinearLayout popup; TextView caption; HudView hud;
 MediaPlayer player; File audioFile; JSONArray history=new JSONArray();
 public void onCreate(){super.onCreate();instance=this;wm=getSystemService(WindowManager.class);
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("wake","Jarvis voice listening",NotificationManager.IMPORTANCE_LOW));
  startForeground(42,notification("Preparing offline voice model…"));
  power=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"jarvis:voice");power.acquire();
  tts=new TextToSpeech(this,result->{ttsReady=result==TextToSpeech.SUCCESS;if(ttsReady){tts.setLanguage(Locale.ENGLISH);tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){ui.post(()->finishReply(Integer.parseInt(id)));}public void onError(String id){ui.post(()->finishReply(Integer.parseInt(id)));}});}});
  listener=new Thread(this::listenLoop,"jarvis-offline-listener");listener.start();
 }
 Notification notification(String text){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,WakeService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,"wake").setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("JARVIS voice mode").setContentText(text).setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"Stop listening",stop).build()).build();
 }
 void notice(String text){getSystemService(NotificationManager.class).notify(42,notification(text));}
 public int onStartCommand(Intent intent,int flags,int id){if(intent!=null&&"STOP".equals(intent.getAction()))stopSelf();return START_NOT_STICKY;}
 public IBinder onBind(Intent i){return null;}
 File prepareModel()throws Exception {
  File root=new File(getFilesDir(),"offline-model"),dir=new File(root,"vosk-model-small-en-us-0.15"),marker=new File(root,"complete");
  if(marker.exists())return dir;root.mkdirs();
  try(ZipInputStream zip=new ZipInputStream(getAssets().open("voice-model.zip"))){ZipEntry e;byte[] buf=new byte[16384];while((e=zip.getNextEntry())!=null){if(!running)throw new IOException("Stopped");File f=new File(root,e.getName());if(!f.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("Invalid archive");if(e.isDirectory())f.mkdirs();else{f.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(f)){int n;while((n=zip.read(buf))>0)out.write(buf,0,n);}}}}
  if(!new File(dir,"am/final.mdl").exists())throw new IOException("Model missing");marker.createNewFile();return dir;
 }
 void listenLoop(){
  try{model=new Model(prepareModel().getAbsolutePath());ui.post(()->notice("Ready — say Jarvis"));
   while(running){if(paused){Thread.sleep(80);continue;}
    int size=Math.max(AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT),8192);
    AudioRecord mic=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,size);
    Recognizer rec=null;
    try{rec=new Recognizer(model,16000,"[\"jarvis\",\"[unk]\"]");mic.startRecording();short[] buf=new short[1600];long deadline=0;command=false;
     while(running&&!paused){int n=mic.read(buf,0,buf.length);if(n<0)throw new IOException("Microphone unavailable");boolean end=rec.acceptWaveForm(buf,n);String words=new JSONObject(end?rec.getResult():rec.getPartialResult()).optString(end?"text":"partial","");
      if(!command&&Arrays.asList(words.split(" ")).contains("jarvis")){
       if(!Session.valid(this)){paused=true;ui.post(()->{showPopup("Unlock your phone and JARVIS to use commands");notice("Phone locked — actions blocked");ui.postDelayed(()->{if(running){removePopup();resume();}},2500);});break;}
       command=true;rec.close();rec=new Recognizer(model,16000);deadline=SystemClock.elapsedRealtime()+12000;ui.post(()->{showPopup("Listening… speak your command");notice("Listening to your command");});
      }else if(command&&(end&&!words.isEmpty()||SystemClock.elapsedRealtime()>deadline)){
       if(words.isEmpty())words=new JSONObject(rec.getFinalResult()).optString("text","");final String text=words;paused=true;ui.post(()->handle(text));break;
      }
     }
    }finally{try{mic.stop();}catch(Exception ignored){}mic.release();if(rec!=null)rec.close();}
   }
  }catch(Exception e){if(running)ui.post(()->{Toast.makeText(this,"Voice mode stopped. Check microphone permission and restart voice mode.",Toast.LENGTH_LONG).show();stopSelf();});}
  finally{if(model!=null)model.close();}
 }
 public void pause(){paused=true;generation++;if(tts!=null)tts.stop();stopPlayer();}
 public void resume(){if(running){command=false;paused=false;notice("Ready — say Jarvis");}}
 public void screenLocked(){ui.post(()->{generation++;if(tts!=null)tts.stop();stopPlayer();removePopup();paused=true;ui.postDelayed(this::resume,300);});}
 void handle(String raw){
  int ticket=++generation;if(!Session.valid(this)){finishReply(ticket);return;}
  if(raw.trim().isEmpty()){speak("I didn't catch that. Say Jarvis to try again.",ticket);return;}
  showPopup("You: "+raw+"\nThinking…");
  String result=Commands.run(this,raw);
  if(result!=null){if(result.equals("__STOP__")){stopSelf();return;}speak(result,ticket);return;}
  new Thread(()->{String answer;try{answer=AiClient.ask(this,raw,history);}catch(Exception e){answer="AI connection failed. Check your AI settings and service credit.";}final String reply=answer;ui.post(()->{if(running&&ticket==generation&&Session.valid(this))speak(reply,ticket);else finishReply(ticket);});},"jarvis-answer").start();
 }
 void speak(String text,int ticket){
  if(ticket!=generation||!running)return;showPopup(text);if(hud!=null)hud.label="SPEAKING";notice("Speaking — listening resumes afterwards");
  String key=Vault.get(this,"fish");if(key.isEmpty()){localSpeech(text,ticket);return;}
  new Thread(()->{File f=null;try{f=FishSpeech.fetch(this,text);final File sound=f;ui.post(()->{if(!running||ticket!=generation||!Session.valid(this)){sound.delete();finishReply(ticket);return;}try{audioFile=sound;player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());player.setDataSource(sound.getAbsolutePath());player.setOnCompletionListener(p->{stopPlayer();finishReply(ticket);});player.setOnErrorListener((p,w,e)->{stopPlayer();localSpeech(text,ticket);return true;});player.prepare();player.start();}catch(Exception e){stopPlayer();localSpeech(text,ticket);}});}catch(Exception e){if(f!=null)f.delete();ui.post(()->{if(ticket==generation){notice("Fish unavailable — using phone voice");localSpeech(text,ticket);}});}},"jarvis-fish").start();
 }
 void localSpeech(String text,int ticket){if(!running||ticket!=generation)return;if(!Session.valid(this)){finishReply(ticket);return;}if(!ttsReady||tts.speak(text.substring(0,Math.min(text.length(),3500)),TextToSpeech.QUEUE_FLUSH,null,String.valueOf(ticket))==TextToSpeech.ERROR)finishReply(ticket);}
 void finishReply(int ticket){if(!running||ticket!=generation)return;ui.postDelayed(()->{if(running&&ticket==generation){removePopup();resume();}},700);}
 void stopPlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}if(audioFile!=null){audioFile.delete();audioFile=null;}}
 void showPopup(String text){
  if(!android.provider.Settings.canDrawOverlays(this)){notice(text);return;}
  if(popup==null){popup=new LinearLayout(this);popup.setOrientation(1);popup.setPadding(16,16,16,16);popup.setBackgroundColor(0xf0081724);hud=new HudView(this);popup.addView(hud,new LinearLayout.LayoutParams(-1,dp(190)));caption=new TextView(this);caption.setTextColor(Color.WHITE);caption.setTextSize(16);caption.setMaxLines(7);popup.addView(caption);LinearLayout row=new LinearLayout(this);Button chat=new Button(this);chat.setText("Open chat");chat.setOnClickListener(v->{pause();removePopup();try{startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}catch(Exception e){resume();}});row.addView(chat);Button close=new Button(this);close.setText("Dismiss");close.setOnClickListener(v->{pause();removePopup();resume();});row.addView(close);popup.addView(row);
   WindowManager.LayoutParams p=new WindowManager.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(340)),-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,android.graphics.PixelFormat.TRANSLUCENT);p.gravity=Gravity.CENTER;
   try{wm.addView(popup,p);}catch(Exception e){popup=null;notice("Overlay blocked — open Jarvis");}
  }if(caption!=null)caption.setText(text);
 }
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 public void removePopup(){if(popup!=null){try{wm.removeView(popup);}catch(Exception ignored){}popup=null;}}
 public void onDestroy(){running=false;paused=true;generation++;ui.removeCallbacksAndMessages(null);removePopup();stopPlayer();if(tts!=null)tts.shutdown();if(power!=null&&power.isHeld())power.release();if(instance==this)instance=null;super.onDestroy();}
}
