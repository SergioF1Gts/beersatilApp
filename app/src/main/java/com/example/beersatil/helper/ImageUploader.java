package com.example.beersatil.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUploader {
    // Reemplaza con tus datos de Cloudinary (no son secretos: el preset es "unsigned")
    private static final String CLOUD_NAME = "knsaomho";
    private static final String UPLOAD_PRESET = "beersatil";

    private static final int MAX_SIDE = 1280;
    private static final int JPEG_QUALITY = 80;
    private static final String TAG = "ImageUploader";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onProgress(int percent);
        void onSuccess(String url);
        void onError(String message);
    }

    public static void upload(Context context, Uri uri, Callback cb) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                byte[] jpeg = compress(app, uri);
                String url = send(jpeg, cb);
                MAIN.post(() -> cb.onSuccess(url));
            } catch (Exception e) {
                Log.e(TAG, "Fallo al subir imagen", e);
                MAIN.post(() -> cb.onError("No se pudo subir la imagen. Revisa tu conexión."));
            }
        });
    }

    private static byte[] compress(Context ctx, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IllegalStateException("Imagen inválida");

        int rotation = 0;
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(in); // requiere API 24+
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
            else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
            else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
        } catch (Exception ignored) {}

        float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bmp.getWidth(), bmp.getHeight()));
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postRotate(rotation);
        Bitmap out = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        out.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, bos);
        return bos.toByteArray();
    }

    private static String send(byte[] data, Callback cb) throws Exception {
        String boundary = "----BeerSatil" + UUID.randomUUID();
        String crlf = "\r\n";
        byte[] head = ("--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"upload_preset\"" + crlf + crlf
                + UPLOAD_PRESET + crlf
                + "--" + boundary + crlf
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"" + crlf
                + "Content-Type: image/jpeg" + crlf + crlf).getBytes(StandardCharsets.UTF_8);
        byte[] tail = (crlf + "--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8);
        long total = (long) head.length + data.length + tail.length;

        URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setFixedLengthStreamingMode(total);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(head);
            int written = 0;
            int lastPct = -1;
            while (written < data.length) {
                int len = Math.min(16 * 1024, data.length - written);
                out.write(data, written, len);
                written += len;
                final int pct = (int) (written * 100L / data.length);
                if (pct != lastPct) {
                    lastPct = pct;
                    MAIN.post(() -> cb.onProgress(pct));
                }
            }
            out.write(tail);
            out.flush();
        }

        int code = conn.getResponseCode();
        InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        String body = readAll(stream);
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new Exception("Cloudinary respondió " + code + ": " + body);
        }
        return new JSONObject(body).getString("secure_url");
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        in.close();
        return bos.toString("UTF-8");
    }
    public static String optimized(String url, int width) {
        if (url == null) return null;
        String marker = "/upload/";
        if (url.contains("res.cloudinary.com") && url.contains(marker)) {
            return url.replace(marker, marker + "w_" + width + ",c_limit,q_auto/");
        }
        return url;
    }
}