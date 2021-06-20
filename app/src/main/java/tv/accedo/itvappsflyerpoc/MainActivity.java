package tv.accedo.itvappsflyerpoc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static MainActivity activity;
    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        myWebView.loadUrl("file:///android_asset/index.html");
        //myWebView.loadUrl("https://www.selatesting.com/PRO/HBOVODAFONE/hbo-ctv/examples/itv-poc/index.html");
    }

    public static final String MARKET_AMAZON_URL = "amzn://apps/android?asin=";
    public static final String WEB_AMAZON_URL = "http://www.amazon.com/gp/mas/dl/android?asin=";
    public static final String ANDROID_LINK_SCHEMA = "itvapp:";
    // britbox asin = "B084RCQNF6";
    // iTV asin = "B00PH7XGQG";
    // SHA256: 0C:B9:10:EA:0C:4B:7B:8F:B8:50:96:EB:B5:02:49:DF:08:20:38:AF:AA:B8:78:07:5B:FD:4F:E8:1C:30:FD:0A

    private static void openOnAmazonMarket(Context context, String asin) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_AMAZON_URL + asin));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_AMAZON_URL + asin));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(LOG_TAG, "Initial URL: " + url);
            Uri link = Uri.parse(url);
            Context mContext = getApplicationContext();
            String host = "";
            String asin = "";

            // At startup, let "onelink" URL to be processed as a regular URL to allow redirection
            if (url.contains("onelink.me")) {
                Log.d(LOG_TAG, "Open a OneLink URL: " + url);
                return false;
            }

            // URL returned by AppsFlyer contains "intent://".
            // Therefore, proper redirection url must be extracted to be used afterwards.
            if (url.contains("intent://")) {
                // af_dp example: itvapp://mainactivity
                // af_android_custom_url example: amzn://apps/android?asin=B00PH7XGQG

                // af_dp represents the Android Link to open an App
                String af_dp = link.getQueryParameter("af_dp");
                int androidLinkIndex = af_dp.lastIndexOf("/");
                String appId = af_dp.substring(androidLinkIndex + 1);
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(appId);

                // App is installed, set the URL to open it
                if (launchIntent != null) {
                    url = af_dp;
                    Log.d(LOG_TAG, "URL obtained from Intent URL af_id param: " + url);
                }
                // App is not installed, set the URL to open the App Store
                else {
                    // af_android_custom_url represents the Android Link to open the App Store
                    url = link.getQueryParameter("af_android_custom_url");
                    Log.d(LOG_TAG, "URL obtained from Intent URL af_android_custom_url param: " + url);
                }
            }

            // Check if the new Android URL belongs to the app currently running
            if (url.contains(ANDROID_LINK_SCHEMA)) {
                Log.d(LOG_TAG, "Package already running. Reload...");
                Toast.makeText(mContext, "Reload running webapp", Toast.LENGTH_SHORT).show();
                WebView myWebView = (WebView) findViewById(R.id.webview);
                myWebView.loadUrl("file:///android_asset/index.html");
                return false;
            }
            // Google Play Logic
            else if (url.contains("play.google.com")) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    link = Uri.parse("http://" + url);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW, link);
                try {
                    intent.setPackage("com.android.vending");
                    startActivity(intent);
                    Log.d(LOG_TAG, "Open app at Google Play Store: " + link);
                    return true;
                } catch (Exception e) {
                    Log.d(LOG_TAG, "error: " + e.getMessage());
                }
            }
            // Amazon Store Logic
            else if(url.contains("asin=")) {
                String[] appUrl = url.split("asin=");
                host = appUrl[0];
                asin = appUrl.length > 1 ? appUrl[1] : "";

                if (asin.length() > 0) {
                    Log.d(LOG_TAG, "Try to use Amazon Store Link: " + host + "asin=" + asin);
                    String app = "";
                    switch(asin) {
                        case "B00PH7XGQG": app = "iTV Hub"; break;
                        case "B084RCQNF6": app = "Britbox"; break;
                    }
                    String msg = app + " " + getString(R.string.no_apps_to_handle_intent);
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

                    openOnAmazonMarket(mContext, asin);

                    return true;
                }
            }
            // DeepLink to App Logic
            else if (url.contains("app://")) {
                Log.d(LOG_TAG, "Open an Android URL: " + url);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);

                return true;
            }

            // No App nor Google play link, treat it as a regular URL
            Log.d(LOG_TAG, "Open a regular URL: " + url);
            return false;
        }
    }
}