package ar.tablerista.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/index.html";
    private static final int FILE_REQUEST = 41;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF0A0C0F);
        getWindow().setNavigationBarColor(0xFF0A0C0F);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0A0C0F);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setTextZoom(100);
        s.setSupportZoom(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception e) {
                    toast("No hay una app para abrir ese enlace");
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "Abrir tablero"), FILE_REQUEST);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    toast("No se pudo abrir el selector de archivos");
                    return false;
                }
            }
        });

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(START_URL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript(
                "(function(){var m=[].slice.call(document.querySelectorAll('.modal')).filter(function(x){return !x.hidden});" +
                "if(m.length){m[m.length-1].hidden=true;return 'closed'}" +
                "var i=document.querySelector('.insp.open');if(i){var c=document.getElementById('iclose');if(c)c.click();return 'closed'}" +
                "return 'none'})()",
                value -> {
                    if (!"\"closed\"".equals(value)) {
                        if (web.canGoBack()) web.goBack();
                        else finish();
                    }
                });
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }

    private static String cleanName(String name) {
        String n = name == null ? "archivo" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        return n.isEmpty() ? "archivo" : n;
    }

    /** Funciones que la página llama como window.AndroidBridge.* */
    private class Bridge {

        @JavascriptInterface
        public boolean saveFile(String name, String base64, String mime) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String file = cleanName(name);
                boolean image = mime != null && mime.startsWith("image/");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, file);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH,
                            (image ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS) + "/Tablerista");
                    Uri collection = image ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    Uri uri = getContentResolver().insert(collection, v);
                    if (uri == null) throw new Exception("insert");
                    try (OutputStream o = getContentResolver().openOutputStream(uri)) {
                        if (o == null) throw new Exception("stream");
                        o.write(bytes);
                    }
                    toast(image ? "Guardado en Galería › Tablerista" : "Guardado en Descargas › Tablerista");
                } else {
                    File dir = new File(getExternalFilesDir(null), "Tablerista");
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("dir");
                    File f = new File(dir, file);
                    try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                    toast("Guardado en " + f.getAbsolutePath());
                }
                return true;
            } catch (Exception e) {
                toast("No se pudo guardar el archivo");
                return false;
            }
        }

        @JavascriptInterface
        public boolean shareFile(String name, String base64, String mime, String text) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "shared");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("dir");
                File f = new File(dir, cleanName(name));
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
                Uri uri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".files", f);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                if (text != null && !text.isEmpty()) send.putExtra(Intent.EXTRA_TEXT, text);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(Intent.createChooser(send, "Compartir")));
                return true;
            } catch (Exception e) {
                toast("No se pudo compartir");
                return false;
            }
        }
    }
}
