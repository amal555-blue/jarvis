package com.amal.jarvis;
import android.content.*;import android.security.keystore.*;import android.util.Base64;import java.security.*;import javax.crypto.*;import javax.crypto.spec.*;
public class Vault {
 static javax.crypto.SecretKey key() throws Exception {KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);if(!s.containsAlias("jarvis-settings")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("jarvis-settings",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes("GCM").setEncryptionPaddings("NoPadding").build());g.generateKey();}return (javax.crypto.SecretKey)s.getKey("jarvis-settings",null);}
 static void put(Context c,String name,String value)throws Exception{Cipher e=Cipher.getInstance("AES/GCM/NoPadding");e.init(Cipher.ENCRYPT_MODE,key());String data=Base64.encodeToString(e.getIV(),2)+":"+Base64.encodeToString(e.doFinal(value.getBytes("UTF-8")),2);c.getSharedPreferences("vault",0).edit().putString(name,data).commit();}
 static String get(Context c,String name){try{String[] a=c.getSharedPreferences("vault",0).getString(name,"").split(":");Cipher d=Cipher.getInstance("AES/GCM/NoPadding");d.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(a[0],2)));return new String(d.doFinal(Base64.decode(a[1],2)),"UTF-8");}catch(Exception e){return "";}}
}
