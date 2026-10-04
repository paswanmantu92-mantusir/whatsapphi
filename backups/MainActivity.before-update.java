package com.mantusir.whatsapphi;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String HOME_URL =
            "https://projects.mantusir.in";

    private static final String START_URL =
            "https://projects.mantusir.in/p/quick-whatsapp-chat.html?m=1";

    private WebView webView;
    private long downloadId = -1;

    private final BroadcastReceiver downloadReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    long id = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID,
                            -1
                    );

                    if (id == downloadId) {
                        installApk(id);
                    }
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createInterface();

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                    downloadReceiver,
                    new IntentFilter(
                            DownloadManager.ACTION_DOWNLOAD_COMPLETE
                    ),
                    Context.RECEIVER_EXPORTED
            );
        } else {
            registerReceiver(
                    downloadReceiver,
                    new IntentFilter(
                            DownloadManager.ACTION_DOWNLOAD_COMPLETE
                    )
            );
        }

        webView.loadUrl(START_URL);
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Button homeButton = new Button(this);
        homeButton.setText("HOME");
        homeButton.setAllCaps(false);

        homeButton.setOnClickListener(
                v -> webView.loadUrl(HOME_URL)
        );

        root.addView(
                homeButton,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {

                        return handleUrl(
                                request.getUrl().toString()
                        );
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        return handleUrl(url);
                    }
                }
        );

        webView.setDownloadListener(
                new DownloadListener() {

                    @Override
                    public void onDownloadStart(
                            String url,
                            String userAgent,
                            String contentDisposition,
                            String mimeType,
                            long contentLength) {

                        downloadFile(
                                url,
                                userAgent,
                                contentDisposition
                        );
                    }
                }
        );

        root.addView(
                webView,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private boolean handleUrl(String url) {

        if (url == null) {
            return false;
        }

        if (url.startsWith("whatsapp://")
                || url.startsWith("whatsapp-business://")) {

            openExternal(url);
            return true;
        }

        if (url.startsWith("https://wa.me/")
                || url.startsWith("http://wa.me/")
                || url.startsWith("https://api.whatsapp.com/")
                || url.startsWith("http://api.whatsapp.com/")) {

            openExternal(url);
            return true;
        }

        if (url.startsWith("intent://")) {

            openIntentUrl(url);
            return true;
        }

        return false;
    }

    private void openExternal(String url) {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (ActivityNotFoundException e) {

            Toast.makeText(
                    this,
                    "WhatsApp is not installed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openIntentUrl(String url) {

        try {

            Intent intent = Intent.parseUri(
                    url,
                    Intent.URI_INTENT_SCHEME
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to open link",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void downloadFile(
            String url,
            String userAgent,
            String contentDisposition) {

        try {

            DownloadManager.Request request =
                    new DownloadManager.Request(
                            Uri.parse(url)
                    );

            request.setTitle("WhatsApp Hi APK");
            request.setDescription("Downloading APK...");

            request.setNotificationVisibility(
                    DownloadManager.Request
                            .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setMimeType(
                    "application/vnd.android.package-archive"
            );

            if (userAgent != null) {
                request.addRequestHeader(
                        "User-Agent",
                        userAgent
                );
            }

            String fileName = "WhatsApp-Hi-Update.apk";

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
            );

            DownloadManager manager =
                    (DownloadManager)
                            getSystemService(
                                    DOWNLOAD_SERVICE
                            );

            downloadId = manager.enqueue(request);

            Toast.makeText(
                    this,
                    "APK download started",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Download failed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void installApk(long id) {

        DownloadManager manager =
                (DownloadManager)
                        getSystemService(
                                DOWNLOAD_SERVICE
                        );

        Uri apkUri =
                manager.getUriForDownloadedFile(id);

        if (apkUri == null) {

            Toast.makeText(
                    this,
                    "APK not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (Build.VERSION.SDK_INT >= 26
                && !getPackageManager()
                .canRequestPackageInstalls()) {

            Intent settingsIntent =
                    new Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse(
                                    "package:" +
                                    getPackageName()
                            )
                    );

            startActivity(settingsIntent);

            Toast.makeText(
                    this,
                    "Allow APK installation for WhatsApp Hi",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            Intent installIntent =
                    new Intent(Intent.ACTION_VIEW);

            installIntent.setDataAndType(
                    apkUri,
                    "application/vnd.android.package-archive"
            );

            installIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            installIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(installIntent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to start APK installer",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        try {
            unregisterReceiver(downloadReceiver);
        } catch (Exception ignored) {
        }

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
