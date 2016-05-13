package net.goeasyway.easyand.bundle;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;

import net.goeasyway.easyand.bundlemananger.BundleManager;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public abstract class BundleFrameworkApp extends Application {

    private BundleManager bundleManager;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        bundleManager = BundleManager.getInstance();
        bundleManager.init(this);
    }

//    @Override
//    public AssetManager getAssets() {
//        if (bundleManager != null) {
//            /**
//             * 主要针对SDK4.4以上WebView更换了实现方式（Chromium）,
//             * 需要对返回的AssetManager进行判断是否要用Bundle中的；
//             */
//            AssetManager webViewAssetManager = bundleManager.getWebViewAssetManager();
//            if (webViewAssetManager != null) {
//                return webViewAssetManager;
//            }
//        }
//        return super.getAssets();
//    }

}
