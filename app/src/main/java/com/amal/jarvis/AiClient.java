package com.amal.jarvis;
import android.content.*;import java.net.*;import java.io.*;import org.json.*;
public class AiClient {
 public static String ask(Context c,String question,JSONArray history)throws Exception{
 String endpoint=Vault.get(c,"endpoint"),model=Vault.get(c,"model"),token=Vault.get(c,"token");
 if(endpoint.isEmpty()||model.isEmpty()||token.isEmpty())return "Open AI & voice setup and enter your AI endpoint, model and API key to enable intelligent answers.";
 URL url=new URL(endpoint);if(!url.getProtocol().equals("https"))throw new IOException("HTTPS required");
 JSONArray messages=new JSONArray();messages.put(new JSONObject().put("role","system").put("content","You are JARVIS, Amal's helpful study and everyday assistant. Be concise and accurate. Say when unsure. You cannot execute phone actions; never claim you did. Do not invent live information."));for(int i=Math.max(0,history.length()-12);i<history.length();i++)messages.put(history.get(i));messages.put(new JSONObject().put("role","user").put("content",question));
 HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(45000);conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+token);conn.setRequestProperty("Content-Type","application/json");
 try{byte[] body=new JSONObject().put("model",model).put("messages",messages).put("stream",false).toString().getBytes("UTF-8");try(OutputStream o=conn.getOutputStream()){o.write(body);}int code=conn.getResponseCode();if(code!=200)throw new IOException("AI service returned HTTP "+code+". Check endpoint, model, key and service credit.");ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>1048576)throw new IOException("Response too large");b.write(buf,0,n);}}String reply=new JSONObject(b.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");history.put(new JSONObject().put("role","user").put("content",question));history.put(new JSONObject().put("role","assistant").put("content",reply));return reply;}finally{conn.disconnect();}
 }
}
