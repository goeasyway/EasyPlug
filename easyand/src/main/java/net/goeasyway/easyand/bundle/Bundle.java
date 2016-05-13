package net.goeasyway.easyand.bundle;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Parcel;

import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.bundlemananger.BundleUtils;
import net.goeasyway.easyand.utils.ParcelableUtils;

import java.io.File;

import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class Bundle {

    private BundleModule bundleModule;
    private BundleInfo bundleInfo;
    private BundleClassLoader bundleClassLoader;

    public Bundle(BundleInfo info) {
        bundleInfo = info;
    }

    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    public BundleModule getBundleModule() {
        if (bundleModule == null) {
            String apkPath = bundleInfo.getApkPath();
            String libraryPath = bundleInfo.getBundlePath() + "/version" + bundleInfo.getVersion() + "/lib";
            String optimized = bundleInfo.getBundlePath() + "/version" + bundleInfo.getVersion();
            File bundleDataFile = new File(bundleInfo.getBundlePath() + "/data");
            ClassLoader parent = BundleManager.getInstance().getParentClassLoader();
            if ( bundleClassLoader == null) {
                bundleClassLoader = new BundleClassLoader(apkPath, optimized, libraryPath, parent);
            }
            PackageInfo packageInfo = bundleInfo.getPackageInfo();
            Context hostContext = BundleManager.getInstance().getHostContext();
            if (packageInfo == null) {
                packageInfo = getPackageInfo(bundleInfo);
                bundleInfo.setPackageInfo(packageInfo);
            }
            bundleModule = new BundleModule(hostContext, apkPath, bundleDataFile, bundleClassLoader, packageInfo);
        }
        return bundleModule;
    }

    public BundleClassLoader getBundleClassLoader() {
        if (bundleClassLoader == null) {
            String apkPath = bundleInfo.getApkPath();
            String libraryPath = bundleInfo.getBundlePath() + "/version" + bundleInfo.getVersion() + "/lib";
            String optimized = bundleInfo.getBundlePath() + "/version" + bundleInfo.getVersion();
            ClassLoader parent = BundleManager.getInstance().getParentClassLoader();
            bundleClassLoader = new BundleClassLoader(apkPath, optimized, libraryPath, parent);
        }
        return bundleClassLoader;
    }

    public void releasePluginBundle() {
        if (bundleModule != null) {
            bundleModule = null;
        }
    }

    public String getPackageName() {
        return bundleInfo.getPackageName();
    }

    public String getType() {
        return bundleInfo.getType();
    }

    /**
     * 获取PackageInfo信息
     * 如果在数据库中未保存有PackageInfo (Parcelable对像)，则从APK文件解析，并转换为byte[]保存入数据库。
     */
    private PackageInfo getPackageInfo(BundleInfo bundleInfo) {
        PackageInfo packageInfo = null;
        Context hostContext = BundleManager.getInstance().getHostContext();
        Realm realm = Realm.getInstance(hostContext);
        RealmQuery<BundlePackageInfo> query = realm.where(BundlePackageInfo.class);
        BundlePackageInfo info = query.equalTo("packageName", bundleInfo.getPackageName()).findFirst();
        //从数据库获取的数据转换成PackageInfo
        if (info != null) {
            Parcel parcel = ParcelableUtils.unmarshall(info.getInfoData());
            try {
                packageInfo = PackageInfo.CREATOR.createFromParcel(parcel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 从APK文件解析出PackageInfo，并存入数据库中
        if (packageInfo == null) {
            packageInfo =  BundleUtils.parseApk(hostContext, bundleInfo.getApkPath());

            savePackageInfo(bundleInfo, packageInfo, realm);
        }
        realm.close();
        return packageInfo;
    }

    public static void savePackageInfo(BundleInfo bundleInfo, PackageInfo packageInfo, Realm realm) {
        BundlePackageInfo info = new BundlePackageInfo();
        info.setPackageName(bundleInfo.getPackageName());
        info.setInfoData(ParcelableUtils.marshall(packageInfo));

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(info);
        realm.commitTransaction();
    }


}
