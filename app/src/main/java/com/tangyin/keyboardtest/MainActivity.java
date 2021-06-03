package com.tangyin.keyboardtest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import spa.lyh.cn.lib_utils.PixelUtils;
import spa.lyh.cn.lib_utils.translucent.BarUtils;
import spa.lyh.cn.lib_utils.translucent.TranslucentUtils;
import spa.lyh.cn.lib_utils.translucent.listener.OnNavHeightListener;
import spa.lyh.cn.lib_utils.translucent.navbar.NavBarFontColorControler;
import spa.lyh.cn.lib_utils.translucent.statusbar.StatusBarFontColorControler;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ActivityShowAreaControler controler;
    private NestedScrollView nestedScrollView;

    //private int oldPosition;//webview曾经的高度
    //private int currentPosition;//当前scrollview需要显示的高度
    private int scrollHeight;//scrollview高度
    private int topPosition;//光标上沿
    private int bottomPosition;//光标下沿


    private View headView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controler = new ActivityShowAreaControler(this);
        setContentView(R.layout.activity_main);
        nestedScrollView = findViewById(R.id.nsl);
        headView = findViewById(R.id.headView);
        TranslucentUtils.setTranslucentBoth(getWindow());
        BarUtils.autoFitBothBar(this,R.id.status_bar,R.id.nav_bar);
        initWebview();
        setWebviewClient();
        webView.loadUrl("http://testcmsweb.sinoing.net/cmsAppeditor/#/?minHeight=300");
        nestedScrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int mLayerHeight = nestedScrollView.getHeight();
                if (scrollHeight != 0 && mLayerHeight != scrollHeight){
                    //不是初始化，高度发生了改变
                    //重新定位位置
                    new Handler(getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            goScrollPosition(topPosition,bottomPosition);
                        }
                    },150);
                }
                scrollHeight = mLayerHeight;
            }
        });
        PixelUtils.getNavigationBarHeight(this, new OnNavHeightListener() {
            @Override
            public void getHeight(int height, int navbarType) {
                controler.setOffset(PixelUtils.getStatusBarHeight(MainActivity.this) + height);
            }
        });
    }

    private void initWebview(){
        webView = findViewById(R.id.web);
        webView.setHorizontalScrollBarEnabled(false);//水平不显示
        webView.setVerticalScrollBarEnabled(false); //垂直不显示
        WebSettings webSettings = webView.getSettings();
        //屏蔽图片
        //webSettings.setBlockNetworkImage(true);
        // 不支持缩放
        webSettings.setSupportZoom(false);

        // 自适应屏幕大小
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        //使用缓存
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDatabaseEnabled(true);

        //DOM Storage
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        //启动对js的支持
        webSettings.setJavaScriptEnabled(true);
        //启动Autoplay
        //webSettings.setMediaPlaybackRequiresUserGesture(false);
        //对图片大小适配
        webSettings.setUseWideViewPort(true);
        //对文字大小适配
        webSettings.setLoadWithOverviewMode(true);
        // 判断系统版本是不是5.0或之上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //让系统不屏蔽混合内容和第三方Cookie
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
            webSettings.setMixedContentMode(0);//永远允许
        }
        //注入js方法
        webView.addJavascriptInterface(this,"injectedNative");
    }

    private void setWebviewClient(){
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http")){
                    return super.shouldOverrideUrlLoading(view, url);
                }else {
                    //拦截
                    return true;
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view,url);
            }
        });
    }

    @JavascriptInterface
    public void onContentChange(String jsonString) {
        //Toast.makeText(this,jsonString,Toast.LENGTH_SHORT).show();
        try {
            JSONObject json = new JSONObject(jsonString);
            int contentHeihgt = json.getInt("contentHeihgt");//安卓端应该用不着这个属性
            int topPosition = json.getInt("topPosition");
            int bottomPosition = json.getInt("bottomPosition");
            //Log.e("qwer","contentHeihgt:"+contentHeihgt+" topPosition:"+topPosition+" bottomPosition:"+bottomPosition);
            //Log.e("qwer","webview高度:"+webView.getHeight());
            goScrollPosition(topPosition,bottomPosition);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获得正确的偏移量
     * @param topPosition
     * @param bottomPosition
     * @return
     */
    private void goScrollPosition(int topPosition,int bottomPosition){
        this.topPosition = topPosition;
        if (topPosition == bottomPosition){
            //空行没有字
            this.bottomPosition = bottomPosition + 19;
        }else {
            this.bottomPosition = bottomPosition;
        }
        //Log.e("qwer","高度:"+this.bottomPosition);
        //判断文字在上面
        int currentPosition = nestedScrollView.getScrollY();//当前scrollview的偏移量
        int scrollHeight = nestedScrollView.getHeight();//获得scroll的高度
        int trueTop = PixelUtils.dip2px(this,this.topPosition) + headView.getHeight();//加头部固定高度
        int trueBottom = PixelUtils.dip2px(this,this.bottomPosition) + headView.getHeight();
        if (currentPosition > trueTop){
            //当前光标显示在可见区域上面
            nestedScrollView.scrollTo(0,trueTop - PixelUtils.dip2px(this,10));
        }else if ((currentPosition+scrollHeight) < trueBottom){
            //当前光标显示在可见区域下面
            //Log.e("qwer","下面");
            /*Log.e("qwer","trueBottom:"+trueBottom);
            Log.e("qwer","scrollHeight:"+scrollHeight);
            Log.e("qwer","差:"+(trueBottom - scrollHeight));
            Log.e("qwer","Y:"+nestedScrollView.getScrollY());*/
            int position = trueBottom - scrollHeight + PixelUtils.dip2px(this,10);
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    nestedScrollView.scrollTo(0,position);
                }
            },20);
        }
        //return nestedScrollView.getScrollY();//要获取，因为实际偏移量不一定是你传的偏移量
    }

    @Override
    protected void onResume() {
        super.onResume();
        StatusBarFontColorControler.setStatusBarMode(getWindow(),true);
        //NavBarFontColorControler.setNavBarMode(getWindow(),false);
    }
}