package com.example.androidcodeautomation;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView statusText;
    private TextView codeText;
    private TextView logText;
    private AutomationEngine engine;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        statusText = findViewById(R.id.statusText);
        codeText = findViewById(R.id.codeText);
        logText = findViewById(R.id.logText);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0 Mobile Safari/537.36"
        );
        webView.setWebViewClient(new WebViewClient());

        engine = new AutomationEngine(
                this,
                webView,
                new AutomationEngine.Listener() {
                    @Override
                    public void onStatus(String status) {
                        runOnUiThread(() -> statusText.setText(status));
                        appendLog(status);
                    }

                    @Override
                    public void onCode(String code) {
                        runOnUiThread(() -> codeText.setText("Latest code: " + code));
                    }

                    @Override
                    public void onLog(String message) {
                        appendLog(message);
                    }
                }
        );

        startButton.setOnClickListener(v -> {
            acquireWakeLock();
            engine.start();
        });

        stopButton.setOnClickListener(v -> engine.stop());

        appendLog("Ready.");
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "CodeAutomation:ScreenWake"
        );
        wakeLock.acquire();
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String old = logText.getText().toString();
            String next = old + (old.isEmpty() ? "" : "\n") + message;
            if (next.length() > 7000) next = next.substring(next.length() - 7000);
            logText.setText(next);
            logText.post(() -> {
                if (logText.getLayout() != null) {
                    logText.scrollTo(0, logText.getLayout().getLineTop(logText.getLineCount()));
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (engine != null) engine.stop();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
