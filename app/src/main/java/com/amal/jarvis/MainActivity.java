package com.amal.jarvis;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.provider.AlarmClock;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricPrompt;
import java.util.*;

public class MainActivity extends FragmentActivity {
 TextView status; EditText input; LinearLayout controls; SpeechRecognizer recognizer; TextToSpeech voice;
 boolean unlocked=false, ready=false, busy=false, foreground=false; BiometricPrompt prompt;
 HudView hud; TextView chat; org.json.JSONArray history=new org.json.JSONArray();
 java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
 void pauseWake(){if(WakeService.instance!=null)WakeService.instance.pause();}
 void resumeWake(){if(WakeService.instance!=null&&Session.valid(this))WakeService.instance.resume();}

 @Override public void onCreate(Bundle state) {
  super.onCreate(state); getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(24,28,24,24); root.setBackgroundColor(Color.rgb(5,12,20));
  TextView title=new TextView(this); title.setText("J A R V I S"); title.setTextColor(Color.CYAN); title.setTextSize(22); root.addView(title);
 hud=new HudView(this);root.addView(hud,new LinearLayout.LayoutParams(-1,(int)(260*getResources().getDisplayMetrics().density)));
  status=new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(18); status.setPadding(0,32,0,32); root.addView(status);
  button(root,"Unlock assistant",this::authenticate);
  controls=new LinearLayout(this); controls.setOrientation(1); root.addView(controls);
  input=new EditText(this); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.GRAY); input.setHint("Try: open WhatsApp / alarm 06:30 / time"); controls.addView(input);
  button(controls,"Run command",()->run(input.getText().toString()));
  button(controls,"Speak",this::listen);
  button(controls,"AI & voice setup",this::setup);

  button(controls,"Start Jarvis wake word",this::startWake);
  button(controls,"Stop voice session",()->{stopService(new Intent(this,WakeService.class));Session.unlocked=false;unlocked=false;Session.unlocked=false;stopService(new Intent(this,WakeService.class));refresh();});
  chat=new TextView(this);chat.setTextColor(Color.rgb(150,210,225));chat.setTextSize(16);chat.setTextIsSelectable(true);controls.addView(chat);
  button(controls,"Enable floating bubble",()->{
   if(!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));say("Allow display over other apps, then tap this button again.");return;}
   if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);
   startForegroundService(new Intent(this,BubbleService.class));say("Floating bubble enabled. Tap it to open Jarvis.");
  });
  button(controls,"Stop floating bubble",()->stopService(new Intent(this,BubbleService.class)));
  button(controls,"Lock",()->{unlocked=false;Session.unlocked=false;stopService(new Intent(this,WakeService.class));refresh();});
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);unlocked=Session.valid(this);refresh();
  voice=new TextToSpeech(this,result->{ready=result==TextToSpeech.SUCCESS;if(ready)voice.setLanguage(Locale.ENGLISH);
  voice.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){runOnUiThread(()->{hud.label="J.A.R.V.I.S";resumeWake();});}public void onError(String id){runOnUiThread(()->resumeWake());}});});
 }
 void button(LinearLayout parent,String label,Runnable action){Button b=new Button(this); b.setText(label); parent.addView(b); b.setOnClickListener(v->action.run());}
 void refresh(){controls.setVisibility(unlocked?View.VISIBLE:View.GONE);status.setText(unlocked?"Ready. Tap Speak or type a command.":"Locked. Use your phone's supported biometric or screen-lock credential.");}
 void authenticate(){
  prompt=new BiometricPrompt(this, r->runOnUiThread(r),new BiometricPrompt.AuthenticationCallback(){
   @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){unlocked=true;Session.unlocked=true;refresh();}
   @Override public void onAuthenticationError(int code,CharSequence error){unlocked=false;Session.unlocked=false;stopService(new Intent(MainActivity.this,WakeService.class));refresh();status.setText(error);}
  });
  try {prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock JARVIS").setSubtitle("Android verifies your identity").setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL).build());}
  catch(Exception e){status.setText("Set up a phone screen lock or biometric in Android settings first.");}
 }
 void say(String text){if(!Session.valid(this))return;status.setText(text);chat.append("\nJARVIS: "+text+"\n");pauseWake();hud.label="SPEAKING";if(ready){int result=voice.speak(text.substring(0,Math.min(text.length(),3500)),TextToSpeech.QUEUE_FLUSH,null,"jarvis");if(result==TextToSpeech.ERROR)resumeWake();}else resumeWake();}
 void listen(){
  if(!Session.valid(this)||busy)return;
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);return;}
  pauseWake();if(voice!=null)voice.stop();
  listenRaw();
 }

 void listenRaw(){
  if(!unlocked)return;
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);say("Grant microphone permission, then tap Speak again.");return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){say("Install or enable a speech recognition service, or type your command.");return;}
  if(ready)voice.stop();
  if(recognizer!=null)recognizer.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this);
  recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){status.setText("Listening… Speak your command now.");hud.label="LISTENING";} public void onBeginningOfSpeech(){} public void onRmsChanged(float f){hud.energy=Math.max(0,f);} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){} public void onPartialResults(Bundle b){} public void onEvent(int e,Bundle b){}
   public void onError(int error){if(!foreground)return;say("Speech recognition stopped ("+error+"). Tap Speak to retry.");}
   public void onResults(Bundle b){ArrayList<String> list=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(unlocked&&list!=null&&!list.isEmpty()){input.setText(list.get(0));run(list.get(0));}}
  });
  Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-IN");recognizer.startListening(intent);
 }
 void launch(Intent intent){try{startActivity(intent);}catch(Exception e){say("No installed app can handle this action.");}}
 void run(String raw){
  if(!unlocked||!Session.valid(this)||busy)return;
  chat.append("\nYOU: "+raw+"\n");
  String cmd=raw.trim().toLowerCase(Locale.ROOT).replaceFirst("^jarvis[, ]*","");
  if(cmd.equals("time")){say(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new Date()));}
  else if(cmd.equals("date")){say(java.text.DateFormat.getDateInstance().format(new Date()));}
  else if(cmd.equals("settings")){launch(new Intent(Settings.ACTION_SETTINGS));}
  else if(cmd.equals("lock")){unlocked=false;Session.unlocked=false;stopService(new Intent(this,WakeService.class));refresh();}
  else if(cmd.startsWith("search ")){launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(cmd.substring(7)))));}
  else if(cmd.matches("alarm \\d{1,2}:\\d{2}")){
   String[] t=cmd.substring(6).split(":");int h=Integer.parseInt(t[0]),m=Integer.parseInt(t[1]);
   if(h>23||m>59){say("Use an hour from 0 to 23 and minutes from 0 to 59.");return;}
   launch(new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,m).putExtra(AlarmClock.EXTRA_MESSAGE,"JARVIS").putExtra(AlarmClock.EXTRA_SKIP_UI,false));
  }else if(cmd.startsWith("open ")){
   String name=cmd.substring(5).trim();Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
   List<ResolveInfo> matches=new ArrayList<>();for(ResolveInfo r:getPackageManager().queryIntentActivities(query,0))if(r.loadLabel(getPackageManager()).toString().equalsIgnoreCase(name))matches.add(r);
   if(matches.isEmpty()){say("App not found. Use its full launcher name.");return;}
   String[] labels=new String[matches.size()];for(int i=0;i<labels.length;i++)labels[i]=matches.get(i).loadLabel(getPackageManager())+" ("+matches.get(i).activityInfo.packageName+")";
   if(matches.size()==1){ResolveInfo r=matches.get(0);launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)));}
   else new AlertDialog.Builder(this).setTitle("Choose app").setItems(labels,(d,i)->{ResolveInfo r=matches.get(i);launch(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(r.activityInfo.packageName,r.activityInfo.name)));}).show();
  }else {busy=true;pauseWake();hud.label="THINKING";status.setText("Thinking…");worker.execute(()->{String answer;try{answer=AiClient.ask(this,raw,history);}catch(Exception e){answer=AiClient.failureMessage(this,e);}String reply=answer;runOnUiThread(()->{busy=false;if(Session.valid(this)&&foreground)say(reply);else resumeWake();});});}
 }
 @Override protected void onResume(){super.onResume();foreground=true;unlocked=Session.valid(this);refresh();if(unlocked&&WakeService.pendingWake){WakeService.pendingWake=false;if(WakeService.instance!=null)WakeService.instance.removePopup();new Handler().postDelayed(this::listen,250);}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(Session.valid(this)&&WakeService.pendingWake){WakeService.pendingWake=false;if(WakeService.instance!=null)WakeService.instance.removePopup();listen();}}
 @Override protected void onStop(){super.onStop();foreground=false;if(recognizer!=null)recognizer.cancel();if(voice!=null)voice.stop();if(!busy)resumeWake();}
 @Override protected void onDestroy(){if(prompt!=null)prompt.cancelAuthentication();if(recognizer!=null)recognizer.destroy();if(voice!=null)voice.shutdown();worker.shutdownNow();super.onDestroy();}
 void startWake(){
  if(!Session.valid(this))return;
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},7);return;}
  if(!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},8);return;}
  if(voice!=null)voice.stop();new AlertDialog.Builder(this).setTitle("Enable hands-free Jarvis?").setMessage("Responds to anyone saying Jarvis. This version does not verify your voice. Listening uses battery and a microphone notification. Actions require an authenticated, unlocked phone. Say Jarvis, pause for the popup, then speak.").setPositiveButton("Enable",(d,w)->{startForegroundService(new Intent(this,WakeService.class));status.setText("Preparing offline voice model… Say Jarvis when the notification says ready.");}).setNegativeButton("Cancel",null).show();
 }
 EditText field(LinearLayout box,String hint,String key,boolean secret){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);if(secret)e.setInputType(129);e.setText(Vault.get(this,key));box.addView(e);return e;}
 void setup(){
  if(!Session.valid(this)||busy)return;pauseWake();
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(24,12,24,12);
  TextView help=new TextView(this);help.setText("AI uses your own compatible chat-completions service. Questions go to that HTTPS endpoint; service charges may apply. Keys are encrypted on this phone. Offline wake word needs no key. Fish Audio is optional for spoken replies; reply text is sent to Fish. Voice identity verification is unavailable.");box.addView(help);
  EditText endpoint=field(box,"Full HTTPS chat/completions URL","endpoint",false),model=field(box,"Model ID from your AI provider","model",false),token=field(box,"AI API key","token",true),fish=field(box,"Fish Audio API key (optional)","fish",true),fishVoice=field(box,"Fish voice ID (optional)","fish_voice",false);
  ScrollView scroll=new ScrollView(this);scroll.addView(box);
  new AlertDialog.Builder(this).setTitle("AI & voice setup").setView(scroll).setPositiveButton("Save",(d,w)->{try{String url=endpoint.getText().toString().trim();if(!url.isEmpty()&&!url.startsWith("https://")){say("Use an HTTPS endpoint.");return;}Vault.put(this,"endpoint",url);Vault.put(this,"model",model.getText().toString().trim());Vault.put(this,"token",token.getText().toString().trim());Vault.put(this,"fish",fish.getText().toString().trim());Vault.put(this,"fish_voice",fishVoice.getText().toString().trim());say("Settings saved. Start Jarvis wake word for hands-free mode.");}catch(Exception e){say("Could not save securely.");}}).setNegativeButton("Cancel",(d,w)->resumeWake()).show();
 }
}
