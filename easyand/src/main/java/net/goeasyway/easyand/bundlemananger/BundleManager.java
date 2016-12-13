package net.goeasyway.easyand.bundlemananger;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.View;

import net.goeasyway.easyand.bundle.Bundle;
import net.goeasyway.easyand.bundle.BundleContextThemeWrapper;
import net.goeasyway.easyand.bundle.BundleException;
import net.goeasyway.easyand.bundle.BundleInfo;
import net.goeasyway.easyand.bundle.BundleModule;
import net.goeasyway.easyand.bundle.BundlePackageInfo;
import net.goeasyway.easyand.utils.LogUtils;
import net.goeasyway.easyand.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class BundleManager {
    private static final String TAG = "BundleManager";
    private static BundleManager instance;
    private Context hostContext;
    private Object activityThread;

    /**
     * 正在运行的plugin service
     */
    private SparseArray<String> runningPluginServices = new SparseArray<String>();
    private final int MAX_SERVICE_NUM = 5;
    private final String PLUGIN_SERVICE_CLASSNAME = "net.goeasyway.easyand.bundle.container.BundleServiceContainer$Proxy";


    /**
     * 正在运行的SingleTask模式的activity
     */
    private SparseArray<String> runningSingleTaskActivities = new SparseArray<String>();
    private final int MAX_ACTIVITY_NUM = 5;
    private final String PLUGIN_SINGLETASK_ACTIVITY_CLASSNAME = "net.goeasyway.easyand.bundle.container.ActivityStub$SingleTaskStub";

    private ActivityManager am;

    public static String webViewContextPackageName;

    private volatile Map<String, Bundle> installedBundles = new HashMap<String, Bundle>();

    private  String cachePath;

    private BundleManager() {

    }

    public static BundleManager getInstance() {
        if (instance == null) {
            instance = new BundleManager();
        }
        return instance;
    }

    public void init(Context hostContext) {

        String processName = getCurProcessName(hostContext);
        if (processName != null && processName.contains(":")) {
            Log.i("BundleManager", "Bundle Framework will not start in other process: " + processName);
            return ;
        }
        if (!(hostContext instanceof Application)) {

        }
        this.hostContext = hostContext;

        String rootPath = hostContext.getFilesDir().getAbsolutePath();
        cachePath = rootPath + "/bundles";
        File cacheDir = new File(cachePath);
        if (!cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                throw new IllegalStateException("Unable to create bundles dir");
            }
        }

        Realm.init(hostContext);

        loadBundles();
    }

    private static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : mActivityManager.getRunningAppProcesses()) {
            if (appProcess.pid == pid) {
                return appProcess.processName;
            }
        }
        return null;
    }

    private void loadBundles() {
        ArrayList<BundleInfo> exceptionBundles = new ArrayList<BundleInfo>(2);

        Realm realm = Realm.getDefaultInstance();//Realm.getInstance(hostContext);
        RealmResults<BundleInfo> infos = realm.where(BundleInfo.class).findAll();

        if (infos != null && infos.size() > 0) {
            for (BundleInfo info : infos) {
                File bundlePathFile = new File(info.getBundlePath() + "/version" + info.getVersion());
                if (!bundlePathFile.exists()) {
                    LogUtils.e(TAG, "[loadBundles] exception bundle:" + info.getPackageName());
                    exceptionBundles.add(info);
                    continue;
                }

                Bundle bundle = new Bundle(BundleUtils.copyNewBundleInfo(info));
                installedBundles.put(info.getPackageName(), bundle);
            }
        }

        // 清除掉数据库的残留信息
        for (BundleInfo info : exceptionBundles) {
            removeBundleInfo(info);
        }
        realm.close();
    }

    public Context getHostContext() {
        return hostContext;
    }

    public Object getActivityThread() {
        if (activityThread == null) {
            try {
                activityThread = ReflectUtils.invoke(Class.forName("android.app.ActivityThread"),
                        null, "currentActivityThread");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return activityThread;
    }

    public Bundle getBundleByPackageName(String packageName) {
        return installedBundles.get(packageName);
    }

    /**
     * 获取SparseArray中和className相同值的Index
     * @param array
     * @param className
     * @return
     */
    private int getSparseArrayIndexOfValue(SparseArray<String> array, String className) {
        if (TextUtils.isEmpty(className)) {
            return -1;
        }
        if (array != null && array.size() > 0) {
            for (int i = 0; i < array.size(); i++) {
                Integer key = array.keyAt(i);
                String value = array.get(key);
                if (className.equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public String chooseProxySingleTaskActivity(ComponentName componentName) {
        String result = "";
        if (componentName == null) {
            return result;
        }
        String className = componentName.getClassName();
        if (TextUtils.isEmpty(className)) {
            return result;
        }
        int index = getSparseArrayIndexOfValue(runningSingleTaskActivities, className);
        if (index >= 0 && index < MAX_ACTIVITY_NUM) { //如果此className已运行
            return PLUGIN_SINGLETASK_ACTIVITY_CLASSNAME + String.valueOf(index + 1);
        }
        // 从1~MAX_SINGLETASK中找出一个不在runningPluginServices的keys中的序号
        for (int i = 0; i < MAX_ACTIVITY_NUM; i++) {
            if (runningSingleTaskActivities.valueAt(i) == null ||
                    !(runningSingleTaskActivities.valueAt(i) instanceof String)) {
                runningSingleTaskActivities.put(i, className);
                return PLUGIN_SINGLETASK_ACTIVITY_CLASSNAME + String.valueOf(i + 1);
            }
        }
        LogUtils.i(TAG, "[chooseProxySingleTaskActivity] Don't find can use Proxy Activity.");
        return result;
    }

    public void removeProxySingleTaskActivity(String className) {
        int index = getSparseArrayIndexOfValue(runningSingleTaskActivities, className);
        if (index >= 0) {
            runningSingleTaskActivities.remove(index);
        }
    }

    /**
     * 根据要启动的Service类名获取一个未被使用的Service容器（真正注册的）
     * 目前默认会声名5个PluginProxyService在AndroidManifest.xml
     * PluginProxyService1~PluginProxyService10
     */
    public String chooseProxyService(ComponentName componentName) {
        String result = "";
        if (componentName == null) {
            return result;
        }
        String className = componentName.getClassName();
        if (TextUtils.isEmpty(className)) {
            return result;
        }
        int index = getSparseArrayIndexOfValue(runningPluginServices, className);
        if (index >= 0 && index < MAX_SERVICE_NUM) { //如果此className已运行
            return PLUGIN_SERVICE_CLASSNAME + String.valueOf(index + 1);
        }
        // 从1~10中找出一个不在runningPluginServices的keys中的序号
        for (int i = 0; i < MAX_SERVICE_NUM; i++) {
            if (runningPluginServices.valueAt(i) == null ||
                    !(runningPluginServices.valueAt(i) instanceof String)) {
                runningPluginServices.put(i, className);
                return PLUGIN_SERVICE_CLASSNAME + String.valueOf(i + 1);
            }
        }
        LogUtils.i(TAG, "[chooseProxyService] Can't find free Proxy service.");
        return result;
    }

    /**
     * Bundle Service destroy时要从runningPluginServices中remove
     * @param className
     */
    public void removeProxyService(String className) {
        int index = getSparseArrayIndexOfValue(runningPluginServices, className);
        if (index >= 0) {
            runningPluginServices.remove(index);
        }
    }

    /**
     * 获取Host平台的类加载器；
     * 自定义的Bundle类加载器需要使用它加载一些系统和Host平台上的类
     * @return
     */
    public ClassLoader getParentClassLoader() {
        return getClass().getClassLoader();
    }

    /**
     * 获取Bundle中的View实例
     */
    public View getBundleView(Context context, String packageName, String viewClassName) {
        Bundle bundle = getBundleByPackageName(packageName);
        if (bundle != null) {
            BundleModule module = bundle.getBundleModule();
            ClassLoader classLoader = module.getClassLoader();
            try {
                Class<?> aClass = classLoader.loadClass("android.app.Activity");
                Activity activity = (Activity) aClass.newInstance();
                BundleContextThemeWrapper newBase = new BundleContextThemeWrapper(module.getPulginApplication(), 0);
                newBase.setBundleModule(module);
                //ReflectUtils.invoke(c, activity, "attachBaseContext", new Class[]{Context.class}, new Object[]{newBase});
                ReflectUtils.invoke(ContextThemeWrapper.class, activity, "attachBaseContext",
                        new Class[]{Context.class}, new Object[]{newBase});
//                ReflectUtils.writeField(activity, "mBase", newBase);
                Class<?> cls = classLoader.loadClass(viewClassName);
                Constructor<?> constructor = cls.getDeclaredConstructor(Context.class);
                constructor.setAccessible(true);
                View view = (View) constructor.newInstance(activity);
                return view;
            } catch (Exception e) {
                LogUtils.e("BundleManager", "[getBundleView] error: " + e.toString());
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * 安装、更新和删除Bundle功能 ＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝
     */

    /**
     * 同步方法：生成一个新的Bundle Id
     */
    private synchronized long generateBundleId() {
        long id = -1;
        Realm realm = Realm.getDefaultInstance();//Realm.getInstance(hostContext);
        long count = realm.where(BundleInfo.class).count();
        if (count > 0) {
            id = realm.where(BundleInfo.class).max("id").intValue() + 1;
        } else {
            id = 0;
        }
        if (id >= 0) {
            BundleInfo info = new BundleInfo();
            info.setId(id);
            realm.beginTransaction();
            realm.copyToRealm(info);
            realm.commitTransaction();
        }
        realm.close();
        return id;
    }

    private BundleInfo getBundleInfoFromApk(String apkPath) {
        BundleInfo info = null;
        PackageInfo packageInfo = BundleUtils.parseApk(hostContext, apkPath);
        if (packageInfo != null) {
            info = new BundleInfo();
            info.setPackageName(packageInfo.packageName);
            info.setVersion(packageInfo.versionCode);
            android.os.Bundle metaData = packageInfo.applicationInfo.metaData;
            if (metaData != null) {
                // 解析出Bundle的配置信息
                info.setType((String) metaData.get("net.goeasyway.bundle_type"));
            }
            info.setPackageInfo(packageInfo);
        }
        return info;
    }

    /**
     * 安装Bundle
     */
    private void installBundle(final String apkFilePath) {
        BundleInfo info = getBundleInfoFromApk(apkFilePath);
        if (info == null) {
            return;
        }
        String packageName = info.getPackageName();
        Bundle bundle = installedBundles.get(packageName);
        if (bundle != null) {
            // TODO 已安装过的Bundle如何提示HOST端
            return;
        }

        long bundleId = generateBundleId();
        if (bundleId == -1) {
            return;
        }

        try {
            info.setId(bundleId);
            info.setBundlePath(cachePath + "/bundle" + bundleId);
            info.setApkPath(info.getBundlePath() + "/version" + info.getVersion() + "/bundle.apk");
            extractApkFileToCache(info, apkFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        bundle = new Bundle(info);
        installedBundles.put(info.getPackageName(), bundle);
        //保存安装的BundleInfo到数据库
        saveBundleInfo(info);
        /**
         * 第一次安装Bundle，创建一个DexClassLoader实例，会去做一些优化DEX文件的工作，
         * 在安装时就创建ClassLoader，这样可以达到优化下次使用Bundle时创建ClassLoader的耗时
         */
        bundle.getBundleClassLoader();
    }

    private synchronized void saveBundleInfo(BundleInfo info) {
        if (info == null || info.getId() < 0) {
            return;
        }
        Realm realm = Realm.getDefaultInstance();//Realm.getInstance(hostContext);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(info);
        realm.commitTransaction();
        Bundle.savePackageInfo(info, info.getPackageInfo(), realm);
        realm.close();
    }


    public void asyncInstallBundle(final String apkPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                installBundle(apkPath);
            }
        }).start();
    }


    /**
     * 将Bundle APK文件释放到相应的Bundle Cache目录中
     */
    private void extractApkFileToCache(BundleInfo info, String apkFilePath) throws Exception {
        File bundlePath = new File(info.getBundlePath() + "/version" + info.getVersion());
        // Bundle目录是否存在
        if (!bundlePath.exists()) {
            bundlePath.mkdirs();
        }
        // 复制APK到指定Bundle目录中
        try {
            BundleUtils.copyApk(apkFilePath, info.getApkPath());
        } catch (Exception e) {
            throw new BundleException(BundleException.ERROR_CODE_COPY_FILE_APK, e.getMessage());
        }
        // 复制APK中的so库文件到Bundle指定的LIB目录
        File libPathFile = new File(info.getBundlePath() + "/version" + info.getVersion() + "/lib");
        if (!libPathFile.exists()) {
            libPathFile.mkdirs();
        }
        try {
            BundleUtils.copyLibs(apkFilePath, libPathFile.getAbsolutePath());
        } catch (Exception e) {
            throw new BundleException(BundleException.ERROR_CODE_COPY_FILE_SO, e.getMessage());
        }
        // 创建应用的data目录
        File dataPathFile = new File(info.getBundlePath() + "/data");
        if (!dataPathFile.exists()) {
            dataPathFile.mkdirs();
        }
    }

    private void updateBundle(Bundle bundle, String newApkPath) {
        BundleInfo info = getBundleInfoFromApk(newApkPath);
        if (info == null) {
            return;
        }

        String packageName = info.getPackageName();
        int version = info.getVersion();
        int oldVersion = bundle.getBundleInfo().getVersion();
        if (version == oldVersion) {
            LogUtils.e("updateBundle", "installed [" + packageName + "] version is same with update version " + version);
            return;
        }

        bundle.releasePluginBundle(); //释放PluginModule

        // 删除旧版本
        String path = cachePath + "/bundle" + bundle.getBundleInfo().getId() + "/version" + oldVersion;
        File bundleFile = new File(path);
        BundleUtils.deleteDirectoryTree(bundleFile);
        installedBundles.remove(packageName);

        long bundleId = bundle.getBundleInfo().getId();
        try {
            info.setId(bundleId);
            info.setBundlePath(cachePath + "/bundle" + bundleId);
            info.setApkPath(info.getBundlePath() + "/version" + info.getVersion() + "/bundle.apk");
            extractApkFileToCache(info, newApkPath);
        } catch (Exception e) {
            LogUtils.e("updateBundle", "[" + packageName + "] extractApkFileToCache error:" + e.getMessage());
            return;
        }
        Bundle newBundle = new Bundle(info);
        installedBundles.put(info.getPackageName(), newBundle);
        //保存安装的BundleInfo到数据库
        saveBundleInfo(info);
    }

    /**
     * 更新Bundle
     * @param bundle
     * @param newApkPath Bundle apk路径
     */
    public void asyncUpdateBundle(final Bundle bundle, final String newApkPath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateBundle(bundle, newApkPath);
            }
        }).start();
    }

    /**
     * 卸载Bundle
     * @param bundle
     */
    public void asyncUnInstallBundle(final Bundle bundle) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String packageName = bundle.getPackageName();
                if (TextUtils.isEmpty(packageName)) {
                    return;
                }

                bundle.releasePluginBundle(); //释放PluginModule

                String path = cachePath + "/bundle" + bundle.getBundleInfo().getId();
                File bundleFile = new File(path);
                BundleUtils.deleteDirectoryTree(bundleFile);
                removeBundleInfo(bundle.getBundleInfo());
                installedBundles.remove(packageName);
            }
        }).start();
    }

    private synchronized void removeBundleInfo(BundleInfo info) {
        if (info == null) {
            return;
        }
        Realm realm = Realm.getDefaultInstance();//Realm.getInstance(hostContext);
        RealmResults<BundleInfo> results = realm.where(BundleInfo.class).equalTo("packageName", info.getPackageName()).findAll();
        RealmResults<BundlePackageInfo> packageInfos = realm.where(BundlePackageInfo.class).equalTo("packageName", info.getPackageName()).findAll();
        realm.beginTransaction();
        if (results != null && results.size() > 0) {
            results.clear();
        }
        if (packageInfos != null && packageInfos.size() > 0) {
            packageInfos.clear();
        }
        realm.commitTransaction();
        realm.close();
    }

    public void installOrUpgradeAssetsBundles() {
        try {
            final String[] bundleNames = hostContext.getAssets().list("bundles");
            if (bundleNames == null) {
                return;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BundleUtils.copyAssetsBundlesToPhone(hostContext);
                    String sdPath = BundleUtils.getDefaultBundleFilePath();
                    File files = new File(sdPath);
                    File[] fileList = files.listFiles();
                    if (fileList == null) {
                        return;
                    }
                    for (File file : fileList) {
                        String apkPath = file.getAbsolutePath();
                        BundleInfo info = getBundleInfoFromApk(apkPath);
                        Bundle bundle = installedBundles.get(info.getPackageName());
                        if (bundle != null) {
                            updateBundle(bundle, apkPath);
                        } else {
                            installBundle(apkPath);
                        }
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
