package com.amal.jarvis;
import android.content.Context;import org.json.JSONObject;import java.io.*;import java.net.*;
final class FishSpeech {
 static File fetch(Context c,String text)throws Exception{
  HttpURLConnection conn=(HttpURLConnection)new URL("https://api.fish.audio/v1/tts").openConnection();File file=null;
  try{conn.setRequestMethod("POST");conn.setConnectTimeout(15000);conn.setReadTimeout(45000);conn.setInstanceFollowRedirects(false);conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+Vault.get(c,"fish"));conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("model","s2.1-pro-free");JSONObject body=new JSONObject().put("text",text.substring(0,Math.min(text.length(),3500))).put("format","mp3");String id=Vault.get(c,"fish_voice");if(!id.isEmpty())body.put("reference_id",id);try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes("UTF-8"));}if(conn.getResponseCode()!=200)throw new IOException("Speech service unavailable");file=File.createTempFile("fish-",".mp3",c.getCacheDir());try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>8*1024*1024)throw new IOException("Audio too large");out.write(b,0,n);}}return file;
  }catch(Exception e){if(file!=null)file.delete();throw e;}finally{conn.disconnect();}
 }
}
