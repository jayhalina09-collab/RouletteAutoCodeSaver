package com.example.androidcodeautomation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;

public class AutomationEngine {

    public interface Listener {
        void onStatus(String status);
        void onCode(String code);
        void onLog(String message);
    }

    private final Context context;
    private final WebView webView;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running = false;
    private boolean waitingForResult = false;

    public AutomationEngine(Context context, WebView webView, Listener listener) {
        this.context = context;
        this.webView = webView;
        this.listener = listener;
    }

    public void start() {
        running = true;
        waitingForResult = false;
        listener.onStatus("Status: Automation Started. Scanning DOM...");
        scanAndExecuteFlow();
    }

    public void stop() {
        running = false;
        waitingForResult = false;
        handler.removeCallbacksAndMessages(null);
        listener.onStatus("Status: Automation Stopped.");
    }

    private void scanAndExecuteFlow() {
        if (!running) return;

        // Extract DOM text to inspect page state
        String js =
                "(function() {" +
                "  var bodyText = (document.body ? document.body.innerText || document.body.textContent : '').toUpperCase();" +
                "  if (bodyText.indexOf('CONGRATS NANALO KA KATROPA!') !== -1 || bodyText.indexOf('WINNING CODE') !== -1) {" +
                "    var inputs = document.querySelectorAll('input');" +
                "    for (var i = 0; i < inputs.length; i++) {" +
                "      var val = (inputs[i].value || '').trim();" +
                "      var m = val.match(/\\b[A-Z0-9]{12}\\b/);" +
                "      if (m) return JSON.stringify({state: 'WINNER', code: m[0]});" +
                "    }" +
                "    var match = bodyText.match(/\\b[A-Z0-9]{12}\\b/);" +
                "    if (match) return JSON.stringify({state: 'WINNER', code: match[0]});" +
                "  }" +
                "  if (bodyText.indexOf('TRY AGAIN') !== -1) {" +
                "    return JSON.stringify({state: 'TRY_AGAIN'});" +
                "  }" +
                "  if (bodyText.indexOf('SPIN THE GOLDEN WHEEL') !== -1) {" +
                "    return JSON.stringify({state: 'GOLDEN_WHEEL'});" +
                "  }" +
                "  if (bodyText.indexOf('SPIN') !== -1) {" +
                "    return JSON.stringify({state: 'SPIN_READY'});" +
                "  }" +
                "  return JSON.stringify({state: 'SCANNING'});" +
                "})()";

        webView.evaluateJavascript(js, value -> {
            if (!running) return;

            String unquoted = unquote(value);

            if (unquoted.contains("\"state\":\"WINNER\"")) {
                String code = extractJsonField(unquoted, "code");
                if (code != null && !code.isEmpty()) {
                    listener.onStatus("Code Found: " + code);
                    listener.onCode(code);
                    waitingForResult = false;
                    // Restart flow after code extraction
                    handler.postDelayed(this::scanAndExecuteFlow, 3000);
                    return;
                }
            }

            if (unquoted.contains("\"state\":\"TRY_AGAIN\"")) {
                listener.onStatus("Result: Try Again detected. Restarting flow...");
                waitingForResult = false;
                // Native tap on "TRY AGAIN" button (Center Bottom)
                performNativeTouch(0.5f, 0.85f);
                handler.postDelayed(this::scanAndExecuteFlow, 3000);
                return;
            }

            if (!waitingForResult) {
                if (unquoted.contains("\"state\":\"GOLDEN_WHEEL\"")) {
                    listener.onStatus("Action: Tapping 'SPIN THE GOLDEN WHEEL'...");
                    // Center screen native tap for Golden Wheel trigger
                    performNativeTouch(0.5f, 0.65f);
                    handler.postDelayed(this::scanAndExecuteFlow, 2500);
                    return;
                }

                if (unquoted.contains("\"state\":\"SPIN_READY\"")) {
                    listener.onStatus("Action: Tapping 'SPIN'. Waiting 15s for results...");
                    waitingForResult = true;
                    // Center native tap for SPIN button
                    performNativeTouch(0.5f, 0.70f);
                    // Schedule result inspection after 15-second wheel spin delay
                    handler.postDelayed(this::scanAndExecuteFlow, 15000);
                    return;
                }
            }

            // Retry state scan if no active triggers matched
            handler.postDelayed(this::scanAndExecuteFlow, 2000);
        });
    }

    /**
     * Sends hardware-level MotionEvents to the WebView using relative X/Y screen percentages.
     */
    private void performNativeTouch(float xPercent, float yPercent) {
        if (webView == null || webView.getWidth() == 0 || webView.getHeight() == 0) return;

        float x = webView.getWidth() * xPercent;
        float y = webView.getHeight() * yPercent;

        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        MotionEvent downEvent = MotionEvent.obtain(
                downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0
        );
        MotionEvent upEvent = MotionEvent.obtain(
                downTime, eventTime + 100, MotionEvent.ACTION_UP, x, y, 0
        );

        webView.dispatchTouchEvent(downEvent);
        webView.dispatchTouchEvent(upEvent);

        downEvent.recycle();
        upEvent.recycle();
    }

    private String unquote(String input) {
        if (input == null) return "";
        if (input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1);
        }
        return input.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String extractJsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\":\"";
        int start = json.indexOf(key);
        if (start == -1) return null;
        start += key.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
