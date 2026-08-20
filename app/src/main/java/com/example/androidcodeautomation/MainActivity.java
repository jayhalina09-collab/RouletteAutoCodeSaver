package com.example.androidcodeautomation;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
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
    private AutomationEngine engine;
    private CodeStorage storage;
    private PowerManager.WakeLock wakeLock;
    private PermissionRequest pendingPermissionRequest;

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize file storage target
        storage = new CodeStorage(this);

        // Request runtime permissions on launch
        requestNativePermissions();

        statusText = findViewById(R.id.statusText);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        webView = findViewById(R.id.webView);

        // Configure WebSettings for full feature compatibility
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0 Mobile Safari/537.36"
        );

        webView.setWebViewClient(new WebViewClient());

        // Handle camera permissions for face verification scans
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

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
                pendingPermissionRequest = null;
            }
        });

        engine = new AutomationEngine(
                this,
                webView,
                new AutomationEngine.Listener() {
                    @Override
                    public void onStatus(String status) {
                        runOnUiThread(() -> statusText.setText(status));
                    }

                    @Override
                    public void onCode(String code) {
                        // Automatically append code to Downloads/save_codeseoulette.txt
                        if (storage != null) {
                            storage.appendCode(code);
                        }
                        runOnUiThread(() -> 
                            Toast.makeText(MainActivity.this, "Saved: " + code, Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onLog(String message) {
                        // Handled via standard Logcat
                    }
                }
        );

        startButton.setOnClickListener(v -> {
            acquireWakeLock();
            engine.start();
        });

        stopButton.setOnClickListener(v -> {
            releaseWakeLock();
            engine.stop();
        });

        statusText.setText("Status: Ready.");
    }

    private void requestNativePermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingPermissionRequest != null) {
                    pendingPermissionRequest.grant(pendingPermissionRequest.getResources());
                    pendingPermissionRequest = null;
                }
                if (webView != null) webView.reload();
            } else {
                if (pendingPermissionRequest != null) {
                    pendingPermissionRequest.deny();
                    pendingPermissionRequest = null;
                }
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_DIM_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "CodeAutomation:ScreenWake"
            );
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
        if (engine != null) engine.stop();
        releaseWakeLock();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
