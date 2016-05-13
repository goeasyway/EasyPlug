package net.goeasyway.easyand.bundle.container;

import android.app.Application;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;

import net.goeasyway.easyand.bundle.Bundle;
import net.goeasyway.easyand.bundle.BundleContextWrapper;
import net.goeasyway.easyand.bundle.BundleModule;
import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.bundlemananger.BundleUtils;
import net.goeasyway.easyand.utils.LogUtils;
import net.goeasyway.easyand.utils.ReflectUtils;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 插件Service的代理容器，用来接管插件Service的生命周期。
 * Create Date: 2016/5/12
 */
public class BundleServiceContainer extends Service {

    /**
     * 注册5个代理容器给Bundle Service使用
     * 如果你的项目需要更多的service，可以自己添加，并修改BundleManager中的设置的最大Service数量的值
     */
    public static class Proxy1 extends BundleServiceContainer {
    }
    public static class Proxy2 extends BundleServiceContainer {
    }
    public static class Proxy3 extends BundleServiceContainer {
    }
    public static class Proxy4 extends BundleServiceContainer {
    }
    public static class Proxy5 extends BundleServiceContainer {
    }

	private Service service;
    private String className; //真正要运行的Bundle的service
	private BundleManager bundleManager;

	@Override
	public IBinder onBind(Intent intent) {
		if (service == null) {
			loadBundleService(intent);
		}
		if (service != null) {
			return service.onBind(getBundleIntent(intent));
		}
		return null;
	}
	
	@Override
	public void onRebind(Intent intent) {
		if (service != null) {
			service.onRebind(getBundleIntent(intent));
		}
		super.onRebind(intent);
	}
	
	@Override
	public void unbindService(ServiceConnection conn) {
		if (service != null) {
			service.unbindService(conn);
			return;
		}
		super.unbindService(conn);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
        bundleManager = BundleManager.getInstance();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (service == null) {
			loadBundleService(intent);
		}
		if (service != null) {
			return service.onStartCommand(getBundleIntent(intent), flags, startId);
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		if (service != null) {
			LogUtils.i("BundleServiceContainer", "onStart [" + className + "]");
			service.onStart(getBundleIntent(intent), startId);
			return;
		}
		super.onStart(intent, startId);
	}
	
	@Override
	public void onLowMemory() {
		if (service != null) {
			service.onLowMemory();
		}
		super.onLowMemory();
	}
	
	@Override
	public void onTrimMemory(int level) {
		if (service != null) {
			service.onTrimMemory(level);
		}
		super.onTrimMemory(level);
	}
	
	@Override
	public void onDestroy() {
		if (service != null) {
			LogUtils.i("BundleServiceContainer", "onDestroy [" + className + "]");
			bundleManager.removeProxyService(className);
			service.onDestroy();
		}
		super.onDestroy();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (service != null) {
			service.onConfigurationChanged(newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * 从代理封装的Intent获取启动Service的源Intent
	 */
	private Intent getBundleIntent(Intent intent) {
		if (intent == null) {
			return null;
		}
		Intent bundleIntent = intent.getParcelableExtra(BundleUtils.EXTRA_BUNDLE_INTENT);
		return bundleIntent;
	}
	
	private void loadBundleService(Intent intent) {
		if (intent == null) {
			return;
		}
		try {
			ComponentName componentName = intent.getParcelableExtra("componentName");
			className = componentName.getClassName();
			String packageName = componentName.getPackageName();
			Bundle bundle = bundleManager.getBundleByPackageName(packageName);
			if (bundle == null || bundle.getBundleModule() == null) {
				LogUtils.e("BundleServiceContainer", "[loadBundleService] bundle =" + bundle +
                        " packageName[" + packageName + "] className[" + className + "]");
				return;
			}
            BundleModule bundleModule = bundle.getBundleModule();
			if (bundleModule == null) {
				LogUtils.e("BundleServiceContainer", "[loadBundleService] bundleModule =" + bundleModule);
				return;
			}

            Context newBase = new BundleContextWrapper(bundleModule.getPulginApplication());
			ClassLoader classLoader = bundle.getBundleClassLoader();
			try {
				Class<?> c = classLoader.loadClass(className);
				service = (Service) c.newInstance();
                // 获取attach方法需要的参数
				Object activityThread = ReflectUtils.readField(this, "mThread");
				IBinder token = ReflectUtils.readField(this, "mToken");
				Object activityManager = ReflectUtils.readField(this, "mActivityManager");
				// 调用attach方法
				ReflectUtils.invoke(Service.class, service, "attach",
						new Class[]{Context.class, activityThread.getClass(), String.class, IBinder.class, Application.class, Object.class},
						new Object[]{newBase, activityThread, BundleServiceContainer.class.getName(), token, bundleModule.getPulginApplication(), activityManager});
				// 调用onCreate
                service.onCreate();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
