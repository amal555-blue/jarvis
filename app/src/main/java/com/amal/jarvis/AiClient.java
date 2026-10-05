package com.amal.jarvis;
import android.content.*;import java.net.*;import java.io.*;import org.json.*;
public class AiClient {
 public static String ask(Context c,String question,JSONArray history)throws Exception{
 String endpoint=Vault.get(c,"endpoint"),model=Vault.get(c,"model"),token=Vault.get(c,"token");
 if(endpoint.isEmpty()||model.isEmpty()||token.isEmpty())return "Open AI & voice setup and enter your AI endpoint, model and API key to enable intelligent answers.";
 URL url=new URL(endpoint);if(!url.getProtocol().equals("https"))throw new IOException("HTTPS required");
 JSONArray messages=new JSONArray();messages.put(new JSONObject().put("role","system").put("content","You are JARVIS, Amal's helpful study and everyday assistant. Be concise and accurate. Say when unsure. You cannot execute phone actions; never claim you did. Do not invent live information."));for(int i=Math.max(0,history.length()-12);i<history.length();i++)messages.put(history.get(i));messages.put(new JSONObject().put("role","user").put("content",question));
 HttpURLConnection conn=(HttpURLConnection)url.openConnection();conn.setInstanceFollowRedirects(false);conn.setConnectTimeout(15000);conn.setReadTimeout(45000);conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setRequestProperty("Authorization","Bearer "+token);conn.setRequestProperty("Content-Type","application/json");
 try{byte[] body=new JSONObject().put("model",model).put("messages",messages).put("stream",false).toString().getBytes("UTF-8");try(OutputStream o=conn.getOutputStream()){o.write(body);}int code=conn.getResponseCode();if(code<200||code>=300)throw new ServiceException(serviceError(code,conn.getErrorStream(),token));ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=conn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>1048576)throw new IOException("Response too large");b.write(buf,0,n);}}String reply=new JSONObject(b.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");history.put(new JSONObject().put("role","user").put("content",question));history.put(new JSONObject().put("role","assistant").put("content",reply));return reply;}finally{conn.disconnect();}
 }
 private static final class ServiceException extends IOException {
  ServiceException(String message){super(message);}
 }
 private static String serviceError(int code,InputStream stream,String token){
  String detail="";
  if(stream!=null)try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buf=new byte[1024];int n;
   while(out.size()<16384&&(n=in.read(buf,0,Math.min(buf.length,16384-out.size())))!=-1)out.write(buf,0,n);
   JSONObject body=new JSONObject(out.toString("UTF-8"));
   JSONObject error=body.optJSONObject("error");
   if(error!=null)detail=error.optString("message","");
   else if(body.opt("error") instanceof String)detail=body.optString("error","");
  }catch(Exception ignored){}
  detail=detail.replace(token,"[hidden]").replaceAll("AIza[A-Za-z0-9_-]+","[hidden]").replaceAll("sk-[A-Za-z0-9_-]+","[hidden]").replaceAll("[\\p{Cntrl}]+"," ").trim();
  if(detail.length()>700)detail=detail.substring(0,700)+"…";
  String hint;
  switch(code){
   case 400:hint="The provider rejected the request. Check the model and request settings.";break;
   case 401:hint="The API key was rejected. Check the key for this provider.";break;
   case 403:hint="Access was denied. Check API access, key restrictions and regional availability.";break;
   case 404:hint="The endpoint or model was not found. Check both in your provider's console.";break;
   case 429:hint="The provider's rate limit or quota was reached. Wait and check your quota.";break;
   default:hint=code>=500?"The AI provider is temporarily unavailable. Try again later.":"The provider rejected the request. Check your AI settings.";
  }
  return "AI error HTTP "+code+". "+hint+(detail.isEmpty()?"":" Provider: "+detail);
 }
 public static String describeError(Exception error){
  if(error instanceof ServiceException)return error.getMessage();
  if(error instanceof SocketTimeoutException)return "AI request timed out. Check your connection and try again.";
  if(error instanceof UnknownHostException)return "Cannot reach the AI server. Check your internet connection and endpoint hostname.";
  if(error instanceof javax.net.ssl.SSLException)return "Could not establish a secure AI connection. Check the endpoint and phone date and time.";
  if(error instanceof JSONException)return "The AI service returned an unexpected response. Check that your endpoint supports chat completions.";
  return "AI connection failed. Check your internet connection and HTTPS chat-completions endpoint.";
 }
}
