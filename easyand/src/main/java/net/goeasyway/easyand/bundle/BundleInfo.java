package net.goeasyway.easyand.bundle;

import android.content.pm.PackageInfo;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class BundleInfo extends RealmObject {

    @PrimaryKey
    private long id;
    private String packageName;
    private String apkPath;
    private int version;
    private String bundlePath;
    private String type; // TODO 添加Lib库作为Bundle，目前只支持APK作为Bundle
    @Ignore
    private PackageInfo packageInfo;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getApkPath() {
        return apkPath;
    }

    public void setApkPath(String apkPath) {
        this.apkPath = apkPath;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getBundlePath() {
        return bundlePath;
    }

    public void setBundlePath(String bundlePath) {
        this.bundlePath = bundlePath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

}
