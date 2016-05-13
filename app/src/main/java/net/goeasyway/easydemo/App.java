package net.goeasyway.easydemo;

import net.goeasyway.easyand.bundle.BundleFrameworkApp;
import net.goeasyway.easyand.bundlemananger.BundleManager;

/**
 * Created by goeasyway.net on 2016/2/5.
 */
public class App extends BundleFrameworkApp {

    @Override
    public void onCreate() {
        super.onCreate();
        BundleManager.getInstance().installOrUpgradeAssetsBundles();
    }
}
