package com.example.androidcodeautomation;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CodeStorage {

    private static final String TAG = "CodeStorage";
    private static final String FILE_NAME = "save_codeseoulette.txt";
    private final Context context;

    public CodeStorage(Context context) {
        this.context = context;
    }

    public synchronized void appendCode(String code) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = "[" + timestamp + "] " + code + "\n";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(entry);
        } else {
            saveViaLegacyStorage(entry);
        }
    }

    // Modern Android (Android 10+) using MediaStore
    private void saveViaMediaStore(String content) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            // Check if file already exists in Downloads to append
            Uri externalUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            Uri fileUri = null;

            // Open output stream in append mode if possible, else create new
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            fileUri = context.getContentResolver().insert(externalUri, values);

            if (fileUri != null) {
                OutputStream os = context.getContentResolver().openOutputStream(fileUri, "wa"); // 'wa' for write/append
                if (os != null) {
                    os.write(content.getBytes());
                    os.flush();
                    os.close();
                }

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                context.getContentResolver().update(fileUri, values, null, null);
                Log.d(TAG, "Saved code via MediaStore: " + content);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving code via MediaStore", e);
        }
    }

    // Legacy Android (Android 9 and below)
    private void saveViaLegacyStorage(String content) {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File file = new File(downloadsDir, FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file, true); // true = append
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
            Log.d(TAG, "Saved code via legacy storage: " + content);
        } catch (Exception e) {
            Log.e(TAG, "Error saving code via legacy storage", e);
        }
    }
}
