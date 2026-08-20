package com.example.androidcodeautomation;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomationEngine {

    public interface Listener {
        void onStatus(String status);
        void onCode(String code);
        void onLog(String message);
    }

    private static final String URL = "https://qrco.de/bgOyCW";
    private static final long SPIN_WAIT = 15000L;

    private final Context context;
    private final WebView webView;
    private final Listener listener;
    private final SharedPreferences prefs;
    private final Set<String> knownCodes = new HashSet<>();
    private final android.os.Handler handler = new android.os.Handler();

    private boolean running = false;
    private boolean startedFlow = false;

    public AutomationEngine(Context context, WebView webView, Listener listener) {
        this.context = context.getApplicationContext();
        this.webView = webView;
        this.listener = listener;

        prefs = this.context.getSharedPreferences("roulette_codes", Context.MODE_PRIVATE);
        knownCodes.addAll(prefs.getStringSet("roulette_saved", new HashSet<>()));

        webView.addJavascriptInterface(new JsBridge(), "AndroidAutomation");
    }

    public void start() {
        if (running) return;
        running = true;
        listener.onStatus("Opening roulette page...");
        openPage();
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        listener.onStatus("Stopped");
    }

    private void openPage() {
        if (!running) return;

        startedFlow = false;

        webView.post(() -> {
            webView.stopLoading();
            webView.loadUrl(URL);
        });

        handler.postDelayed(this::scanPage, 3000);
    }

    private void scanPage() {
        if (!running) return;

        String js =
            "(function(){var a=[];" +
            "document.querySelectorAll('*').forEach(function(e){" +
            "var t=(e.innerText||e.textContent||'').trim();" +
            "if(t)a.push(t);});" +
            "return JSON.stringify(a);})()";

        webView.evaluateJavascript(js, value -> {
            String text = unquote(value);

            if (!startedFlow && contains(text, "SPIN THE GOLDEN WHEEL")) {
                listener.onStatus("Clicking SPIN THE GOLDEN WHEEL...");
                clickText("SPIN THE GOLDEN WHEEL");
                startedFlow = true;
                handler.postDelayed(this::clickSpin, 3000);
                return;
            }

            if (contains(text, "CONGRATS NANALO KA!")) {
                listener.onStatus("Winner detected. Reading code...");
                String code = extractRouletteCode(text);

                if (code == null) {
                    readCodeWithOCR();
                    return;
                }

                if (code != null) {
                    saveRouletteCode(code);
                    handler.postDelayed(this::openPage, 2000);
                    return;
                }
            }

            if (contains(text, "TRY AGAIN NEXT TIME")) {
                listener.onStatus("Try again. Restarting...");
                handler.postDelayed(this::openPage, 2000);
                return;
            }

            handler.postDelayed(this::scanPage, 1500);
        });
    }

    private void clickSpin() {
        if (!running) return;

        listener.onStatus("Clicking SPIN...");
        clickText("SPIN");

        handler.postDelayed(() -> {
            listener.onStatus("Checking roulette result...");
            scanPage();
        }, SPIN_WAIT);
    }

    private void clickText(String wanted) {
        String js =
            "(function(){var n=document.querySelectorAll('button,a,input,div,span');" +
            "for(var i=0;i<n.length;i++){" +
            "var t=(n[i].innerText||n[i].textContent||n[i].value||'').trim();" +
            "if(t.toLowerCase().includes('" + wanted.toLowerCase() + "')){" +
            "n[i].click();return 'ok';}}" +
            "return 'no';})()";

        webView.evaluateJavascript(js, null);
    }

    private String extractRouletteCode(String text) {
        Pattern p = Pattern.compile("\\b[A-Z0-9]{12}\\b");
        Matcher m = p.matcher(text.replace("\\n"," "));
        while(m.find()) {
            String c = m.group();
            if(c.matches(".*[A-Z].*") && c.matches(".*\\d.*"))
                return c;
        }
        return null;
    }


    private void readCodeWithOCR() {
        webView.post(() -> {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);

            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener(result -> {
                        String code = extractRouletteCode(result.getText());
                        if (code != null) {
                            saveRouletteCode(code);
                            handler.postDelayed(this::openPage, 2000);
                        } else {
                            listener.onLog("OCR found no valid 12 character code");
                            handler.postDelayed(this::scanPage, 1500);
                        }
                    })
                    .addOnFailureListener(e -> {
                        listener.onLog("OCR error: " + e.getMessage());
                        handler.postDelayed(this::scanPage, 1500);
                    });
        });
    }

    private void saveRouletteCode(String code) {
        if(knownCodes.contains(code)) {
            listener.onLog("Duplicate skipped: " + code);
            return;
        }

        knownCodes.add(code);
        prefs.edit().putStringSet("roulette_saved", new HashSet<>(knownCodes)).apply();

        try {
            FileOutputStream fos = context.openFileOutput(
                    "Save_codesroulette.txt",
                    Context.MODE_APPEND
            );

            String line = code + " - " +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date()) + "\n";

            fos.write(line.getBytes());
            fos.close();

        } catch(Exception e) {
            listener.onLog("File error: " + e.getMessage());
        }

        listener.onCode(code);
        listener.onLog("Saved roulette code: " + code);
        listener.onStatus("Saved. Starting next spin...");
    }

    private boolean contains(String a,String b){
        return a.toLowerCase(Locale.US)
                .contains(b.toLowerCase(Locale.US));
    }

    private String unquote(String v){
        if(v==null)return "";
        if(v.startsWith("\"")&&v.endsWith("\""))
            v=v.substring(1,v.length()-1);

        return v.replace("\\\"","\"")
                .replace("\\n","\n")
                .replace("\\\\","\\");
    }

    public class JsBridge {
        @JavascriptInterface
        public void log(String msg){
            listener.onLog(msg);
        }
    }
}
