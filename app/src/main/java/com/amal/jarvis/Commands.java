package com.amal.jarvis;
import android.content.*;import android.content.pm.*;import android.provider.*;import android.net.Uri;import java.util.*;
final class Commands {
 static String run(Context c,String raw){
  if(!Session.valid(c))return "Unlock your phone first.";
  String s=raw.trim().toLowerCase(Locale.ROOT).replaceFirst("^jarvis[, ]*","");
  if(s.equals("stop listening")||s.equals("stop voice mode"))return "__STOP__";
  if(s.equals("time")||s.contains("what time"))return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new Date());
  if(s.equals("date")||s.contains("today's date"))return java.text.DateFormat.getDateInstance().format(new Date());
  Intent i=null;String reply="Opening.";
  if(s.equals("settings")||s.equals("open settings"))i=new Intent(Settings.ACTION_SETTINGS);
  else if(s.startsWith("search "))i=new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/search?q="+Uri.encode(s.substring(7))));
  else if(s.startsWith("open ")){String name=s.substring(5).trim();List<ResolveInfo> matches=new ArrayList<>();for(ResolveInfo info:c.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0))if(info.loadLabel(c.getPackageManager()).toString().equalsIgnoreCase(name))matches.add(info);if(matches.isEmpty())return "App not found. Say its full launcher name.";if(matches.size()>1)return "Several apps have that name. Please open it from your phone.";ResolveInfo info=matches.get(0);i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(info.activityInfo.packageName,info.activityInfo.name));reply="Opening "+name;}
  if(i==null)return null;try{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return reply;}catch(Exception e){return "Android could not open that app. Open Jarvis and try again.";}
 }
}
