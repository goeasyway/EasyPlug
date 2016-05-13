package net.goeasyway.easyand.bundlemananger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.view.LayoutInflater;

import net.goeasyway.easyand.bundle.Bundle;
import net.goeasyway.easyand.bundle.BundleInfo;
import net.goeasyway.easyand.bundle.container.ActivityStub;
import net.goeasyway.easyand.utils.ReflectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description:
 * Create Date: 2016/5/12
 */
public class BundleUtils {
//Extra
    public static final String EXTRA_BUNDLE_INTENT = "extra_bundle_intent";
    public static final String CPU_ABI = android.os.Build.CPU_ABI;

    /**
     * 从APK文件解析PackageInfo信息
     */
    public static PackageInfo parseApk(Context hostContext, String apkPath) {
        PackageManager packageManager = hostContext.getPackageManager();
        int flags = 0xFFFFFFFF;
        PackageInfo info = packageManager.getPackageArchiveInfo(apkPath, flags);
        if (info == null) {
            /**
             * 去掉获取签名信息到PackageInfo
             * 在SDK5.0后的版本，如果不签名的APK getPackageArchiveInfo会返回Null
             */
            info = packageManager.getPackageArchiveInfo(apkPath, flags ^ PackageManager.GET_SIGNATURES);
        }
        return info;
    }

    public static boolean useHostSystemService(String serviceName) {
        if (Context.WIFI_SERVICE.equals(serviceName) || Context.LOCATION_SERVICE.equals(serviceName)
                || Context.TELEPHONY_SERVICE.equals(serviceName)
                || Context.CLIPBOARD_SERVICE.equals(serviceName)
                || Context.INPUT_METHOD_SERVICE.equals(serviceName)) {
            return true;
        }
        return false;
    }

    /**
     * 获取封装后的指向ProxyActivity容器的Intent
     */
    public static Intent getActivityStubIntent(Intent intent) {
        Intent stubIntent;
        Bundle bundle = null;
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            String packageName = componentName.getPackageName();
            bundle = BundleManager.getInstance().getBundleByPackageName(packageName);
        }
        if (bundle != null) {
            int launchMode = bundle.getBundleModule().getActivityLaunchMode(componentName.getClassName());
            if (launchMode == ActivityInfo.LAUNCH_SINGLE_TASK) { // singleTask模式的Activity
                stubIntent = changeToSingleTaskPluginIntent(intent);
            } else {
                // 启动Bundle的Activity
                stubIntent = changeToPluginIntent(intent, ActivityStub.class.getName());
            }
        } else {
            // 调用平台外部的Activity
            stubIntent = intent;
        }
        return stubIntent;
    }

    public static Intent changeToPluginIntent(Intent intent, String className) {
        Intent stubIntent = new Intent();
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            stubIntent.setClassName(BundleManager.getInstance().getHostContext(), className);
            stubIntent.putExtra("componentName", componentName);
            stubIntent.putExtra(EXTRA_BUNDLE_INTENT, intent);
            /**
             * 在此添加上FLAG_ACTIVITY_NEW_TASK会影响到Activity的onActivityResult，
             * 使得这个方法在startActivityForResult后立即就被调用
             */
//			stubIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            stubIntent.setFlags(intent.getFlags());
            return stubIntent;
        }
        return intent;
    }

    public static Intent changeToSingleTaskPluginIntent(Intent intent) {
        Intent stubIntent = new Intent();
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            BundleManager bundleManager = BundleManager.getInstance();
            //选择一个可用的SingleTask的Activity类名
            String className = bundleManager.chooseProxySingleTaskActivity(componentName);
            if (TextUtils.isEmpty(className)) {
                // 如果无法获取空闲的SingleTask的Activity类名，那么就使用默认模式的Activity类名
                className = ActivityStub.class.getName();
            }
            stubIntent.setClassName(BundleManager.getInstance().getHostContext(), className);
            stubIntent.putExtra("componentName", componentName);
            stubIntent.putExtra(EXTRA_BUNDLE_INTENT, intent);
            stubIntent.setFlags(intent.getFlags());
            return stubIntent;
        }
        return intent;
    }

    public static Intent changeToPluginServiceIntent(Intent intent) {
        Intent proxyIntent = new Intent();
        ComponentName componentName = intent.getComponent();
        if (componentName != null) {
            BundleManager bundleManager = BundleManager.getInstance();
            //选择一个代理service
            String className = bundleManager.chooseProxyService(componentName);
            if (TextUtils.isEmpty(className)) {
                return intent;
            }
            proxyIntent.setClassName(BundleManager.getInstance().getHostContext(), className);
            proxyIntent.putExtra("componentName", componentName);
            proxyIntent.putExtra(EXTRA_BUNDLE_INTENT, intent);
            proxyIntent.setFlags(intent.getFlags());
            return proxyIntent;
        }
        return intent;
    }

    /**
     * 获取默认安装的bundle的文件路径
     */
    public static String getDefaultBundleFilePath() {
        Context context = BundleManager.getInstance().getHostContext();
        return context.getFilesDir().toString() + File.separator
                + "defaultBundles" + File.separator;
    }

    /**
     * 把assets下的bundle文件拷贝到data目录
     */
    public static boolean copyAssetsBundlesToPhone(Context context) {
        try {
            String defaultBundleFilePath = getDefaultBundleFilePath();
            File file = new File(defaultBundleFilePath);
            // 先清空目的路径
            deleteDirectoryTree(file);
            if (!file.exists()) {
                file.mkdirs();
            }

            String[] bundleNames = context.getAssets().list("bundles");
            for (String name : bundleNames) {
                if (name == null || !name.contains(".apk")) {
                    continue;
                }
                copyApk(context.getAssets().open("bundles/" + name), defaultBundleFilePath + name);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BundleInfo copyNewBundleInfo(BundleInfo info) {
        BundleInfo result = new BundleInfo();
        result.setId(info.getId());
        result.setVersion(info.getVersion());
        result.setPackageName(info.getPackageName());
        result.setApkPath(info.getApkPath());
        result.setBundlePath(info.getBundlePath());
        result.setType(info.getType());
        return result;
    }

    /**
     * 从文件流复制APK到指定目录
     * @param inputStream
     * @param desApkPath
     * @throws Exception
     */
    public static void copyApk(InputStream inputStream, String desApkPath) throws Exception {
        File outFile = new File(desApkPath);
        if (outFile.exists()) {
            outFile.delete();
        }
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[4096];
        int length = 0;
        while ((length = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        out.close();
        inputStream.close();
    }

    /**
     * 复到APK到指定目录
     * @param apkPath
     * @param desApkPath
     * @throws Exception
     */
    public static void copyApk(String apkPath, String desApkPath) throws Exception {
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            return;
        }
        File outFile = new File(desApkPath);
        if (outFile.exists()) {
            outFile.delete();
        }
        InputStream in = new FileInputStream(apkFile);
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buffer = new byte[4096];
        int length = 0;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        out.close();
        in.close();
    }

    /**
     * 复到SO到指定目录
     * @param apkPath
     * @param libPath
     * @throws Exception
     */
    public static void copyLibs(String apkPath, String libPath) throws Exception {
        ZipFile zipFile = new ZipFile(apkPath);
        Enumeration<?> e = zipFile.entries();
        Set<String> exactLibNames = new HashSet<String>();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();
            if (entryName.endsWith(".so") &&
                    (entryName.contains("armeabi") || entryName.contains(CPU_ABI))) {
                String libName = entryName.substring(entryName.lastIndexOf("/"));
                if (entryName.contains(CPU_ABI)) {
                    //和本机CPU_ABI一样
                    exactLibNames.add(libName);
                } else if (exactLibNames.contains(libName)) {
                    // 非精确的so，可以是armeabi或者armeabi-v7a, 且前面已经加载过精确的so了
                    continue;
                }
                File outFile = new File(libPath + File.separator + libName);
                if (outFile.exists()) {
                    outFile.delete();
                }
                InputStream in = zipFile.getInputStream(entry);
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int length = 0;
                while ((length = in.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                }
                out.close();
                in.close();
            }
        }
        zipFile.close();
    }

    public static boolean deleteDirectoryTree(File target) {
        if (!deleteDirectoryTreeRecursive(target))
        {
            System.gc();
            System.gc();
            return deleteDirectoryTreeRecursive(target);
        }
        return true;
    }

    private static boolean deleteDirectoryTreeRecursive(File target)
    {
        if (!target.exists())
        {
            return true;
        }
        if (target.isDirectory())
        {
            File[] files = target.listFiles();
            if (files != null)
            {
                for (int i = 0; i < files.length; i++)
                {
                    deleteDirectoryTreeRecursive(files[i]);
                }
            }
        }
        return target.delete();
    }

}
