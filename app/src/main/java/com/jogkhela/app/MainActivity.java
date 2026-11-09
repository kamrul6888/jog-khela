package com.jogkhela.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdDisplayListener;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

public class MainActivity extends Activity {

    private WebView web;
    private StartAppAd interstitial;
    private boolean interstitialReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // টেস্ট বিল্ডে (debug) শুধু টেস্ট বিজ্ঞাপন, রিলিজ বিল্ডে আসল বিজ্ঞাপন
        StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG);
        // অ্যাপ খোলার সময়ের স্প্ল্যাশ বিজ্ঞাপন বন্ধ
        StartAppAd.disableSplash();
        // শেষের false = Return ad বন্ধ
        StartAppSDK.init(this, getString(R.string.startio_app_id), false);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#1e3a8a"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // সেরা স্কোর সেভ রাখার জন্য
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new AdBridge(), "AdBridge");

        // স্ট্যাটাস বার ও নিচের বারের নিচে যেন লেখা না ঢাকে
        web.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        interstitial = new StartAppAd(this);
        loadInterstitial();
    }

    // ইন্টারস্টিশিয়াল আগে থেকেই লোড করে রাখা হয়, যাতে সঙ্গে সঙ্গে দেখানো যায়
    private void loadInterstitial() {
        interstitialReady = false;
        interstitial.loadAd(StartAppAd.AdMode.AUTOMATIC, new AdEventListener() {
            @Override public void onReceiveAd(Ad ad) { interstitialReady = true; }
            @Override public void onFailedToReceiveAd(Ad ad) { interstitialReady = false; }
        });
    }

    private void showInterstitial() {
        if (!interstitialReady) { loadInterstitial(); return; }
        interstitial.showAd(new AdDisplayListener() {
            @Override public void adHidden(Ad ad) { loadInterstitial(); }
            @Override public void adDisplayed(Ad ad) { }
            @Override public void adClicked(Ad ad) { }
            @Override public void adNotDisplayed(Ad ad) { loadInterstitial(); }
        });
    }

    private void showRewarded() {
        final StartAppAd rewarded = new StartAppAd(this);
        rewarded.setVideoListener(() -> callJs("onRewardEarned()"));
        rewarded.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override public void onReceiveAd(Ad ad) { rewarded.showAd(); }
            @Override public void onFailedToReceiveAd(Ad ad) { callJs("onRewardFailed()"); }
        });
    }

    private void callJs(String js) {
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    // HTML থেকে AdBridge.showInterstitial() আর AdBridge.showRewarded() ডাকা হয়
    private class AdBridge {
        @JavascriptInterface public void showInterstitial() { runOnUiThread(MainActivity.this::showInterstitial); }
        @JavascriptInterface public void showRewarded() { runOnUiThread(MainActivity.this::showRewarded); }
    }
}
