package net.goeasyway.easyand.bundle;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;

import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.bundlemananger.BundleUtils;

import java.io.File;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description:
 * Create Date: 2016/5/12
 */
public class BundleContextWrapper extends ContextWrapper {
	Context hostContext;

	public BundleContextWrapper(Context base) {
		super(base);
		this.hostContext = BundleManager.getInstance().getHostContext();
	}

	@Override
	public ContentResolver getContentResolver() {
		return hostContext.getContentResolver();
	}
	
	@Override
	public Object getSystemService(String name) {
		if (BundleUtils.useHostSystemService(name)) {
			return hostContext.getSystemService(name);
		}
		return super.getSystemService(name);
	}
	
	@Override
	public File getExternalFilesDir(String type) {
		return hostContext.getExternalFilesDir(type);
	}
	
	@Override
	public File[] getExternalFilesDirs(String type) {
		return hostContext.getExternalFilesDirs(type);
	}
	
	@Override
	public File getExternalCacheDir() {
		return hostContext.getExternalCacheDir();
	}
	
	@Override
	public File[] getExternalCacheDirs() {
		return hostContext.getExternalCacheDirs();
	}
}
