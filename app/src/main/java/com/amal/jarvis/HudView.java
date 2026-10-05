package com.amal.jarvis;
import android.content.*;import android.graphics.*;import android.view.*;import android.os.*;
public class HudView extends View {
 Paint p=new Paint(3);public String label="J.A.R.V.I.S";public float energy=0;long start=SystemClock.uptimeMillis();
 public HudView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
 protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.85f,t=(SystemClock.uptimeMillis()-start)/45f;c.drawColor(Color.rgb(3,12,20));p.setStyle(Paint.Style.STROKE);p.setColor(0xff27d9f4);p.setStrokeWidth(1.5f);p.setShadowLayer(8+energy,0,0,0xff168da6);
 for(int j=0;j<5;j++){float a=r*(1-j*.13f);p.setAlpha(70+j*32);c.drawCircle(x,y,a,p);c.save();c.rotate(t*(j%2==0?1:-1)+j*52,x,y);p.setStrokeWidth(j==2?5:2);for(int k=0;k<4;k++)c.drawArc(x-a,y-a,x+a,y+a,k*90,45+j*4,false,p);c.restore();}
 p.setStrokeWidth(2);p.setAlpha(150);for(int i=0;i<90;i++){double a=Math.toRadians(i*4);float inner=r*.91f;c.drawLine(x+(float)Math.cos(a)*inner,y+(float)Math.sin(a)*inner,x+(float)Math.cos(a)*r*.95f,y+(float)Math.sin(a)*r*.95f,p);}p.clearShadowLayer();p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(r*.12f);p.setTypeface(Typeface.MONOSPACE);p.setAlpha(255);c.drawText(label,x,y+5,p);if(isShown())postInvalidateDelayed(33);
 }
}
