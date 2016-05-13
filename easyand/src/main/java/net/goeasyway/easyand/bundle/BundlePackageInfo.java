package net.goeasyway.easyand.bundle;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class BundlePackageInfo extends RealmObject {
    @PrimaryKey
    private String packageName;
    private byte[] infoData;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public byte[] getInfoData() {
        return infoData;
    }

    public void setInfoData(byte[] infoData) {
        this.infoData = infoData;
    }
}
