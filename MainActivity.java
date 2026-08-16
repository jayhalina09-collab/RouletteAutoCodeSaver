package com.example.rouletteautosaver;

import android.app.*;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {

WebView web;
TextView status;
String url="https://qrco.de/bgOyCW";

@Override
public void onCreate(Bundle b){
super.onCreate(b);
LinearLayout l=new LinearLayout(this);
l.setOrientation(LinearLayout.VERTICAL);

Button start=new Button(this);
start.setText("START AUTO MODE");

status=new TextView(this);
status.setText("Stopped");

web=new WebView(this);
web.getSettings().setJavaScriptEnabled(true);

l.addView(start);
l.addView(status);
l.addView(web,new LinearLayout.LayoutParams(-1,0,1));

setContentView(l);

start.setOnClickListener(v->startLoop());
}

void startLoop(){
status.setText("Monitoring...");
web.loadUrl(url);
watch();
}

void watch(){
new Handler().postDelayed(()->{
web.evaluateJavascript(
"(function(){return document.body.innerText})()",
r->{
if(r.contains("NANALO") && r.matches(".*[A-Z0-9]{12}.*")){
status.setText("WIN CODE DETECTED");
}
});
watch();
},3000);
}
}