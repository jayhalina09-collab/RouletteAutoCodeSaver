package com.example.androidcodeautomation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;
    private TextView statusText;
    private PowerManager.WakeLock wakeLock;
    private PermissionRequest pendingPermissionRequest;

    // Handler and Runnable for scheduled tasks
    private final Handler loopHandler = new Handler(Looper.getMainLooper());
    private Runnable loopTask;
    private boolean isTaskRunning = false;

    // Target URL constant
    private static final String TARGET_URL = "https://www.scanpack.com/ph-ft-goldenticketchancetowin?utm_medium=PRIF&utm_campaign=GoldenSpinWheel&utm_source=PRIF";
    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the screen awake while the Activity is active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestNativePermissions();

        statusText = findViewById(R.id.statusText);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        webView = findViewById(R.id.webView);

        // Enable hardware acceleration layer for smooth rendering
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (statusText != null) {
                    statusText.setText("Status: Page Loaded.");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        pendingPermissionRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    }
                });
            }
        });

        // Load the initial web page to prevent a black screen
        webView.loadUrl(TARGET_URL);

        // Define recurring background execution task
        loopTask = new Runnable() {
            @Override
            public void run() {
                if (!isTaskRunning) return;

                // Non-offending routine UI or state update logic
                if (statusText != null) {
                    statusText.setText("Status: Task Active...");
                }

                // Schedule the next iteration (every 5 seconds)
                loopHandler.postDelayed(this, 5000);
            }
        };

        startButton.setOnClickListener(v -> {
            acquireWakeLock();
            startLoop();
        });

        stopButton.setOnClickListener(v -> {
            stopLoop();
            releaseWakeLock();
        });
    }

    private void startLoop() {
        if (!isTaskRunning) {
            isTaskRunning = true;
            loopHandler.post(loopTask);
            Toast.makeText(this, "Loop Started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLoop() {
        isTaskRunning = false;
        if (loopTask != null) {
            loopHandler.removeCallbacks(loopTask);
        }
        if (statusText != null) {
            statusText.setText("Status: Loop Stopped.");
        }
        Toast.makeText(this, "Loop Stopped", Toast.LENGTH_SHORT).show();
    }

    private void requestNativePermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && pendingPermissionRequest != null) {
                pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
            } else if (pendingPermissionRequest != null) {
                pendingPermissionRequest.deny();
            }
            pendingPermissionRequest = null;
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "App:WakeLock");
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        stopLoop();
        releaseWakeLock();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
