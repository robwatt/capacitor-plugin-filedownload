package com.capacitor.filedownload;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

@CapacitorPlugin(name = "FileDownload", permissions = {
        // SDK VERSIONS 32 AND BELOW
        @Permission(
                alias = FileDownloadPlugin.STORAGE,
                strings = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }
        ),
        /*
        SDK VERSIONS 33 AND ABOVE
        This alias is a placeholder and the PHOTOS alias will be updated to use these permissions
        so that the end user does not need to explicitly use separate aliases depending
        on the SDK version.
         */
        //@Permission(strings = { Manifest.permission.READ_MEDIA_IMAGES }, alias = CameraPlugin.MEDIA)
})
public class FileDownloadPlugin extends Plugin {

    static final String STORAGE = "storage";
    static final String MEDIA = "media";

    private FileDownload implementation = new FileDownload();

    private HttpURLConnection connection = null;

    private String pathStr;

    @PluginMethod
    public void download(PluginCall call) throws JSONException {
        downloadFile(call);
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        call.resolve();
    }

    @PluginMethod
    public void isCanceled(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("isCanceled", connection == null);
        call.resolve(ret);
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        if (getPermissionState(STORAGE) == PermissionState.GRANTED) {
            JSObject permissionsResultJSON = new JSObject();
            permissionsResultJSON.put(STORAGE, "granted");
            call.resolve(permissionsResultJSON);
        } else {
            super.checkPermissions(call);
        }
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (getPermissionState(STORAGE) == PermissionState.GRANTED) {
            JSObject permissionsResultJSON = new JSObject();
            permissionsResultJSON.put(STORAGE, "granted");
            call.resolve(permissionsResultJSON);
        } else {
            requestPermissionForAlias(STORAGE, call, "permissionsCallback");
        }
    }

    @PermissionCallback
    private void permissionsCallback(PluginCall call) {
        this.checkPermissions(call);
    }

    @PluginMethod
    public void openSetting(PluginCall call) {
        Context context = getContext();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
        call.resolve();
    }

    /**
     * Downloads the file using URLConnection.
     *
     * @param call PluginCall used for callback.
     */
    private void downloadFile(final PluginCall call) {
        String urlString = call.getString("url", "");
        final String fileName = call.getString("fileName", "");
        String destination = call.getString("destination", "DOCUMENT");

        assert urlString != null;
        if (urlString.isEmpty()) {
            call.reject("URL is required");
            return;
        }

        assert fileName != null;
        if (fileName.isEmpty()) {
            call.reject("File name is required");
            return;
        }

        URL url;
        InputStream is = null;

        try {
            url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // add headers
            JSObject headers = call.getObject("headers");
            if (headers != null) {
                for (Iterator<String> it = headers.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String value = headers.getString(key);
                    assert value != null;
                    connection.addRequestProperty(key, value);
                }
            }

            // connect
            connection.connect();

            // test if the response is OK
            assert connection.getResponseCode() == HttpURLConnection.HTTP_OK;

            // create destination file
            int totalBytes = connection.getContentLength();
            is = url.openStream();

            assert destination != null;
            File downloadDestination = getDefaultDownloadDestination(destination);
            File file = new File(downloadDestination, fileName);
            pathStr = file.getAbsolutePath();

            // create folders if needed
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    call.reject("Failed to create parent directory");
                    return;
                }
            }

            // download the body
            saveFile(file, totalBytes, is);

            call.resolve(createSuccessResponse());
        } catch (Exception e) {
            Log.e("ERROR DOWNLOADING", "Unable to download" + e.getMessage());
            call.reject("download fail: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    Log.d("ERROR_DOWNLOADING", "Unable to close input stream" + e.getMessage());
                }
                connection = null;
            }
        }
    }

    private void saveFile(File file, long totalBytes, InputStream is) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        byte[] data = new byte[1024];
        long downloadedBytes = 0;
        int length;
        while ((length = is.read(data)) != -1) {
            downloadedBytes += length;
            int progress = (int) (downloadedBytes * 100 / totalBytes);
            notifyProgress(progress);
            fos.write(data, 0, length);
        }
        fos.close();
    }

    private File getDefaultDownloadDestination(String destination) {
        File downloadDestination;
        switch (destination) {
            case "DOCUMENT":
                downloadDestination = getContext().getExternalFilesDir("");
                break;
            case "EXTERNAL":
            case "EXTERNAL_STORAGE":
                downloadDestination = Environment.getExternalStorageDirectory();
                break;
            case "DATA":
                downloadDestination = getContext().getFilesDir();
                break;
            case "CACHE":
                downloadDestination = getContext().getCacheDir();
                break;
            default:
                downloadDestination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                break;
        }

        if (!downloadDestination.exists()) {
            downloadDestination.mkdirs();
        }

        return downloadDestination;
    }

    private void notifyProgress(int progress) {
        JSObject progressObj = new JSObject();
        progressObj.put("progress", progress);
        notifyListeners("downloadProgress", progressObj);
    }

    private JSObject createSuccessResponse() {
        JSObject response = new JSObject();
        response.put("success", true);
        response.put("path", "file://" + pathStr);
        return response;
    }
}
