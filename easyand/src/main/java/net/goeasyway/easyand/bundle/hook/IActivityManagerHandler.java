package net.goeasyway.easyand.bundle.hook;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import net.goeasyway.easyand.bundlemananger.BundleManager;
import net.goeasyway.easyand.bundlemananger.BundleUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class IActivityManagerHandler implements InvocationHandler {
    private static final String TAG = "IActivityManagerHandler";

    Object mBase;
    Context hostContext;

    public IActivityManagerHandler(Object base) {
        mBase = base;
        hostContext = BundleManager.getInstance().getHostContext();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            Intent raw;
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            raw = (Intent) args[index];
            Intent newIntent = BundleUtils.getActivityStubIntent(raw);
            args[index] = newIntent;
        } else if ("registerReceiver".equals(method.getName())) {
            if (args != null && args.length > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    for (int index = 0; index < args.length; index++) {
                        if (args[index] instanceof String) {
                            String callerPackage = (String) args[index];
                            if (BundleManager.getInstance().getBundleByPackageName(callerPackage) != null) {
                                args[index] = hostContext.getPackageName();
                                break;
                            }
                        }
                    }
                }
            }
        } else if ("startService".equals(method.getName())
                || "stopService".equals(method.getName())
                || "bindService".equals(method.getName())) {
            if (args != null && args.length > 0) {
                Intent raw;
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        index = i;
                        break;
                    }
                }
                raw = (Intent) args[index];
                Intent pluginService = BundleUtils.changeToPluginServiceIntent(raw);
                args[index] = pluginService;
            }
        }
        return method.invoke(mBase, args);
    }


}
