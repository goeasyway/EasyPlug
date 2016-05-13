package net.goeasyway.easyand.bundle;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;

import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.utils.LogUtils;
import net.goeasyway.easyand.utils.ReflectUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class BundleModule {
    private static final String TAG = "BundleModule";

    private Context hostContext;
    private Application application;
    private ClassLoader classLoader;
    private Resources resources;
    private AssetManager assetManager;
    private String packageName;
    private PackageInfo packageInfo;
    private ApplicationInfo appInfo;
    private Object activityThread;
    private Object loadedApk;
    private String apkPath;
    private File dataDir;
    private Map<String, ActivityInfo> activityInfoMap = new HashMap<String, ActivityInfo>();
    private int themeResId;
    private Resources.Theme theme;

    public BundleModule(Context context, String apkPath,
                        File bundleDataFile, ClassLoader bundleClassLoader, PackageInfo packageInfo) {
        this.hostContext = context;
        this.apkPath = apkPath;
        this.classLoader = bundleClassLoader;
        this.dataDir = bundleDataFile;
        this.packageInfo = packageInfo;
        this.activityThread = BundleManager.getInstance().getActivityThread();
        createResources(apkPath);
        buildModule();
    }

    private void buildModule() {
        try {
            packageName = packageInfo.packageName;
            appInfo = (ApplicationInfo) parseApplicationInfo(hostContext, packageInfo, 	apkPath, dataDir.getAbsolutePath());
            themeResId = appInfo.theme;
            //LoadedApk
            Object compatibilityInfo = ReflectUtils.invoke(resources.getClass(), resources,"getCompatibilityInfo");
            loadedApk = ReflectUtils.invoke(activityThread.getClass(), activityThread, "getPackageInfoNoCheck",
                    new Class[]{ApplicationInfo.class, Class.forName("android.content.res.CompatibilityInfo")},
                    new Object[]{appInfo, compatibilityInfo});
            ReflectUtils.writeField(loadedApk, "mClassLoader", classLoader);

            // 创建Bundle的Application实例，同时会调用它的onCreate方法
            long time = System.currentTimeMillis();
            makeApplication();
            LogUtils.i(TAG, "[makeApplication] " + packageName + " used Time: " + (System.currentTimeMillis() - time));
            if (packageInfo.activities != null) {
                int length = packageInfo.activities.length;
                for (int i = 0; i < length; i++) {
                    ActivityInfo info = packageInfo.activities[i];
                    activityInfoMap.put(info.name, info);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建Bundle应用的Application实例，并调用它的onCreate方法
     */
    private void makeApplication() {
        //创建Application实例
        Instrumentation instrumentation = ReflectUtils.invoke(activityThread.getClass(), activityThread, "getInstrumentation");

        String appClassName = appInfo.className;
        if (appClassName == null) {
            appClassName = "android.app.Application";
        }
        try {
            Class<?> cls = Class.forName("android.app.ContextImpl");
            Context base = null;
            if (Build.VERSION.SDK_INT >= 21) {
                //用于5.0及以后的版本
                base = ReflectUtils.invoke(cls, null, "createAppContext", new Class[]{activityThread.getClass(), loadedApk.getClass()},
                        new Object[]{activityThread, loadedApk});
            } else {
                base = ReflectUtils.newInstance(cls, null);
                if (base == null) {
                    //SDK4.4.3开始的版本
                    base = ReflectUtils.invoke(cls, null, "createAppContext", new Class[]{activityThread.getClass(), loadedApk.getClass()},
                            new Object[]{activityThread, loadedApk});
                } else {
                    ReflectUtils.invoke(cls, base, "init", new Class[]{loadedApk.getClass(), IBinder.class, activityThread.getClass()},
                            new Object[]{loadedApk, null, activityThread});
                }
            }
            application = instrumentation.newApplication(classLoader, appClassName, base);
            ReflectUtils.writeField(loadedApk, "mApplication", application);
            if (base != null) {
                ReflectUtils.invoke(cls, base, "setOuterContext", new Class[]{Context.class},
                        new Object[]{application});
                BundleContextWrapper newBase = new BundleContextWrapper(base);
                ReflectUtils.writeField(application, "mBase", newBase);
            }
        } catch (Exception e) {
            LogUtils.e("PluginModule", "[" + packageName + "] makeApplication error: " + e.getMessage());
            e.printStackTrace();
        }

        //执行onCreate
        if (application != null) {
            application.onCreate();
        }
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private void createResources(String path) {
        try {
            AssetManager asset = AssetManager.class.newInstance();
            Method method = asset.getClass().getDeclaredMethod("addAssetPath",
                    String.class);
            method.setAccessible(true);
            method.invoke(asset, path);
            assetManager = asset;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Resources res = hostContext.getResources();
        resources = new Resources(assetManager, res.getDisplayMetrics(),
                res.getConfiguration());
    }

    /**
     * 解析和设置ApplicationInfo
     */
    private ApplicationInfo parseApplicationInfo(Context hostContext,
                                                 PackageInfo packageInfo, String apkPath,
                                                 String dataDirPath) {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo != null) {
            applicationInfo.uid = hostContext.getApplicationInfo().uid;
            applicationInfo.sourceDir = apkPath;
            applicationInfo.dataDir = dataDirPath;
            applicationInfo.nativeLibraryDir = dataDir.getAbsolutePath();
        }
        return applicationInfo;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public Resources getResources() {
        return resources;
    }

    public Application getPulginApplication() {
        return application;
    }

    public int getAppThemeResId() {
        return themeResId;
    }

    /**
     * 获取Activity的theme id，如无设置，则返回Application的theme id
     * @return
     */
    public int getThemeResId(String activityClassName) {
        ActivityInfo info = getActivityInfo(activityClassName);
        if (info != null) {
            return info.getThemeResource();
        }
        return themeResId;
    }

    /**
     * 获取Activity的加载模式
     */
    public int getActivityLaunchMode(String activityClassName) {
        ActivityInfo info = getActivityInfo(activityClassName);
        if (info != null) {
            return info.launchMode;
        }
        return ActivityInfo.LAUNCH_MULTIPLE;
    }

    public Resources.Theme getTheme() {
        if (theme == null) {
            theme = resources.newTheme();
        }
        return theme;
    }

    public String getPackageName() {
        return packageName;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public ApplicationInfo getApplicationInfo() {
        return appInfo;
    }

    public ActivityInfo getActivityInfo(String className) {
        return activityInfoMap.get(className);
    }

}
