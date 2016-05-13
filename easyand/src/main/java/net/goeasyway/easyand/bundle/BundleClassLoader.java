package net.goeasyway.easyand.bundle;

import net.goeasyway.easyand.utils.LogUtils;

import dalvik.system.DexClassLoader;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class BundleClassLoader extends DexClassLoader {

    private ClassLoader hostClassLoader; // Host APK的类加载器, 由Android系统为其生成的PathClassLoader

    public BundleClassLoader(String dexPath, String optimizedDirectory,
                             String libraryPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, libraryPath, parent);
        hostClassLoader = parent;
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve)
            throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);
        ClassLoader systemClassLoader = hostClassLoader.getParent();
        if (clazz == null) {
            // 借鉴Felix OSGI的思路：java开头或android系统的类 从parent的类加载器查找 (
            if (className.startsWith("java.") || className.startsWith("javax.")
                    || className.startsWith("android.")) {
                try {
                    clazz = systemClassLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                }
            }
            // 从自身的DEX查找
            if (clazz == null) {
                try {
                    clazz = findClass(className);
                } catch (Exception e) {
                }
            }
            if (clazz == null) {
                try {
                    clazz = hostClassLoader.loadClass(className);
                } catch (ClassNotFoundException e) {
                    clazz = systemClassLoader.loadClass(className);
                }
            }
        }
        if (clazz == null) {
            LogUtils.w("BundleClassLoader", "Can't find class: " + className);
        }
        return clazz;
    }

}
