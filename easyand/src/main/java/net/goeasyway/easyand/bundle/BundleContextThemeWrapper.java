package net.goeasyway.easyand.bundle;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.view.ContextThemeWrapper;

import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.bundlemananger.BundleUtils;

import java.io.File;

public class BundleContextThemeWrapper extends ContextThemeWrapper {

	private Context hostContext;
	private BundleModule module;

	public BundleContextThemeWrapper(Context base, int themeResId) {
		super(base, themeResId);
		this.hostContext = BundleManager.getInstance().getHostContext();
	}
	
	public void setBundleModule(BundleModule module) {
		this.module = module;
	}
	
	@Override
	public Theme getTheme() {
		if (module != null) {
			return module.getTheme();
		}
		return super.getTheme();
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
