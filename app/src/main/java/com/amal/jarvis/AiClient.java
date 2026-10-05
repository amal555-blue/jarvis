package com.amal.jarvis;
import android.content.*;import java.net.*;import java.io.*;import org.json.*;
public class AiClient {
 public static String ask(Context c,String question,JSONArray history)throws Exception{
 String endpoint=Vault.get(c,"endpoint").trim(),model=Vault.get(c,"model").trim(),token=Vault.get(c,"token").trim();
 if(endpoint.isEmpty()||model.isEmpty()||token.isEmpty())return "Open AI & voice setup and enter your AI endpoint, model and API key to enable intelligent answers.";
 URL url=new URL(endpoint);if(!url.getProtocol().equals("https"))throw new IOException("Use a full HTTPS chat/completions endpoint.");
 JSONArray messages=new JSONArray();messages.put(new JSONObject().put("role","system").put("content","You are JARVIS, Amal's helpful study and everyday assistant. Be concise and accurate. Say when unsure. You cannot execute phone actions; never claim you did. Do not invent live information."));for(int i=Math.max(0,history.length()-12);i<history.length();i++)messages.put(history.get(i));messages.put(new JSONObject().put("role","user").put("content",question));
 HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(45000);conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+token);conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("Accept","application/json");
 try{
  byte[] body=new JSONObject().put("model",model).put("messages",messages).put("stream",false).toString().getBytes("UTF-8");
  try(OutputStream o=conn.getOutputStream()){o.write(body);}
  int code=conn.getResponseCode();
  if(code<200||code>=300){
   String detail="";
   try{detail=read(conn.getErrorStream(),16384);}catch(IOException ignored){}
   throw new IOException(httpError(code,detail,token));
  }
  JSONObject response;
  try{response=new JSONObject(read(conn.getInputStream(),1048576));}
  catch(JSONException e){throw new IOException("AI service returned invalid JSON. Check the chat/completions endpoint.");}
  JSONArray choices=response.optJSONArray("choices");
  if(choices==null||choices.length()==0)throw new IOException("AI service returned no answer. Check model support or try rephrasing your question.");
  JSONObject message=choices.getJSONObject(0).optJSONObject("message");
  String reply=message==null?"":message.optString("content","");
  if(reply.trim().isEmpty()||reply.equals("null"))throw new IOException("AI service returned an empty answer. Try rephrasing your question.");
  history.put(new JSONObject().put("role","user").put("content",question));history.put(new JSONObject().put("role","assistant").put("content",reply));return reply;
 }finally{conn.disconnect();}
 }
 static String read(InputStream stream,int limit)throws IOException{
  if(stream==null)return "";
  try(InputStream in=stream;ByteArrayOutputStream b=new ByteArrayOutputStream()){
   byte[] buf=new byte[4096];int n;
   while((n=in.read(buf))!=-1){if(b.size()+n>limit)throw new IOException("AI response too large.");b.write(buf,0,n);}
   return b.toString("UTF-8");
  }
 }
 static String httpError(int code,String body,String token){
  String detail="";
  try{
   Object error=new JSONObject(body).opt("error");
   if(error instanceof JSONObject)detail=((JSONObject)error).optString("message","");
   else if(error instanceof String)detail=(String)error;
  }catch(JSONException ignored){}
  // Only show a provider's structured message, never its full response or request.
  detail=clean(detail,token);
  String hint;
  switch(code){
   case 400:hint="Check the model ID and request settings.";break;
   case 401:hint="API key rejected. Check the key for this provider.";break;
   case 403:hint="Access denied. Check API permissions and service availability.";break;
   case 404:hint="Endpoint or model not found. Check both in AI & voice setup.";break;
   case 429:hint="Rate limit or quota reached. Check your provider's quota and retry later.";break;
   default:hint=code>=500?"AI provider is temporarily unavailable. Retry later.":"Check your AI provider settings.";
  }
  return "AI service returned HTTP "+code+". "+(detail.isEmpty()?"":detail+" ")+hint;
 }
 static String clean(String text,String token){
  if(token!=null&&!token.isEmpty())text=text.replace(token,"[hidden]");
  text=text.replaceAll("AIza[0-9A-Za-z_-]{20,}","[hidden]").replaceAll("sk-[0-9A-Za-z_-]{10,}","[hidden]");
  text=text.replaceAll("[\\p{Cntrl}]+"," ").trim();
  return text.substring(0,Math.min(text.length(),1000));
 }
 public static String failureMessage(Context c,Exception e){
  if(e instanceof SocketTimeoutException)return "AI request timed out. Check your connection and try again.";
  if(e instanceof UnknownHostException)return "Cannot reach the AI server. Check internet access and the endpoint hostname.";
  if(e instanceof javax.net.ssl.SSLException)return "Secure AI connection failed. Check your phone's date, network and HTTPS endpoint.";
  if(e instanceof IOException){
   String message=clean(e.getMessage()==null?"":e.getMessage(),Vault.get(c,"token").trim());
   if(!message.isEmpty())return "AI connection failed. "+message;
  }
  return "AI connection failed. Could not process the provider response. Check the chat/completions endpoint.";
 }
}
