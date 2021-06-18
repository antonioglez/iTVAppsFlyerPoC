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

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        myWebView.loadUrl("file:///android_asset/index.html");
        //myWebView.loadUrl("https://www.selatesting.com/PRO/HBOVODAFONE/hbo-ctv/examples/itv-poc/index.html");
    }

    public static final String MARKET_AMAZON_URL = "amzn://apps/android?asin=";
    public static final String WEB_AMAZON_URL = "http://www.amazon.com/gp/mas/dl/android?asin=";

    private static void openOnAmazonMarket(Context context, String packageName) {

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_AMAZON_URL + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_AMAZON_URL + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }

    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(LOG_TAG, "Initial URL: " + url);
            Uri link = Uri.parse(url);
            Context mContext = getApplicationContext();

            String host = "";
            String asin = "";

            // Google Play Logic
            if ("play.google.com".equals(link.getHost())) {
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
                    String msg = host + " " + getString(R.string.no_apps_to_handle_intent);
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

                    openOnAmazonMarket(mContext, asin);

                    //String britboxAsin = "B084RCQNF6";
                    //String iTVAsin = "B00PH7XGQG";

                    return true;
                }
            }
            /*else if (url.contains("app.tv.accedo")) {
                String[] appUrl = link.toString().replace("https://app.", "").split("/|\\?");
                String host = appUrl[0];
                String asin = appUrl.length > 1 ? appUrl[1] : "";
                Log.d(LOG_TAG, "Try to use Referral Link: " + host + "/" + asin);

                Pattern pattern = Pattern.compile("S.browser_fallback_url=(.*?);", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(link.toString());
                while (matcher.find()) {
                    appUrl = matcher.group(1).replace("https://app.", "").split("/");
                    host = appUrl[0];
                    asin = appUrl.length > 1 ? appUrl[1] : "";
                    Log.d(LOG_TAG, "Try to use Firebase Link: " + host + "/" + asin);
                }

                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(host);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.d(LOG_TAG, "Open installed package: " + launchIntent.getPackage());
                    String msg = host + " " + getString(R.string.apps_to_handle_intent);
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

                    startActivity(launchIntent);

                    if (launchIntent.getPackage().equals(getApplicationContext().getPackageName())) {
                        Log.d(LOG_TAG, "Package already running...");
                        Toast.makeText(mContext, "Reload running webapp", Toast.LENGTH_SHORT).show();
                        WebView myWebView = (WebView) findViewById(R.id.webview);
                        myWebView.loadUrl("javascript:window.location.reload( true )");
                    }

                } else if (asin.length() > 0) {
                    Log.d(LOG_TAG, "Cannot open package: " + host);
                    String msg = host + " " + getString(R.string.no_apps_to_handle_intent);
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

                    openOnAmazonMarket(mContext, asin);

                    //String britboxAsin = "B084RCQNF6";
                    //String iTVAsin = "B00PH7XGQG";
                } else {
                    String msg = getString(R.string.no_apps_found);
                    Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                }
                return true;
            }*/

            // No App nor Google play link, treat it as a regular URL
            Log.d(LOG_TAG, "Open a regular URL: " + url);
            return false;
        }
    }
}