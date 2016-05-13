package net.goeasyway.easyand.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Copyright (C) 2015-present, goeasyway.net
 * Project: EasyPlug an open source Android Plugin Framework
 * Author: goeasyway@163.com
 * Site: www.goeasyway.net
 * Class Description: 
 * Create Date: 2016/5/12
 */
public class ReflectUtils {

    public static <T> T newInstance(String className, Object... args) {
        try {
            Class<?> cls = Class.forName(className);
            Constructor<?> constructor = null;
            if (args == null) {
            	constructor = cls.getDeclaredConstructor();
            } else {
            	constructor = cls.getDeclaredConstructor(getArgClasses(args));
            }
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
			LogUtils.e("ReflectUtils", "[newInstance] className=" + className
					+ " error:" + e.getMessage());
        }
        return null;
    }
    
    public static <T> T newInstance(Class<?> cls, Class<?>[] argClasses, Object... args) {
        try {
            Constructor<?> constructor = null;
            if (args == null) {
            	constructor = cls.getDeclaredConstructor();
            } else {
            	constructor = cls.getDeclaredConstructor(argClasses);
            }
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
			LogUtils.e("ReflectUtils", "[newInstance] cls=" + cls
					+ " error:" + e.getMessage());
        }
        return null;
    }
    
    public static boolean writeField(Object obj, String filedName, Object value) {
        try {
        	Field filed = null;
        	Class<?> cls = obj.getClass();
            while(cls != null && cls != Object.class) {
                try {
                    filed = cls.getDeclaredField(filedName);
                } catch (Exception e) {
                } 
                cls = cls.getSuperclass();            
            }
            if (filed == null) {
                return false;
            }
            filed.setAccessible(true);
            filed.set(obj, value);
            return true;
        } catch (Exception e) {
        	e.printStackTrace();
        }        
        return false;
    }
    
    public static <T> T readField(Object obj, String filedName) {
        try {
            Class<?> cls = obj.getClass();
            Field filed = null;
            while(cls != null && cls != Object.class) {
                try {
                    filed = cls.getDeclaredField(filedName);
                } catch (Exception e) {
                } 
                cls = cls.getSuperclass();            
            }
            filed.setAccessible(true);
            return (T)filed.get(obj);
        } catch (Exception e) {
        	e.printStackTrace();
        }        
        return null;
    }
    
    public static <T> T readStaticField(Class<?> cls, String filedName) {
        try {
            Field filed = null;
            while(cls != null && cls != Object.class) {
                try {
                    filed = cls.getDeclaredField(filedName);
                } catch (Exception e) {
                } 
                cls = cls.getSuperclass();
            }
            filed.setAccessible(true);
            return (T)filed.get(null);
        } catch (Exception e) {
        	e.printStackTrace();
        }        
        return null;
    
    }
    
    private static Class<?>[] getArgClasses(Object... args) {
        int count = args.length;
        Class<?>[] argClasses = new Class<?>[count];
        for (int i = 0; i < count; i++) {
            argClasses[i] = args[i].getClass();
        }
        return argClasses;
    }
    
    public static <T> T invoke(Class<?> cls, Object obj, String methodName) {
        Method method = null;
        try {
            method = cls.getDeclaredMethod(methodName);
        } catch (Exception e) {
        }
        if (method == null) {
            Class<?> superCls = cls.getSuperclass();
            while(superCls != null && superCls != Object.class) {
                try {
                    method = superCls.getMethod(methodName);
                } catch (Exception e) {
                } 
                superCls = superCls.getSuperclass();            
            }
        }
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return (T)method.invoke(obj);
        } catch (Exception e) {
			LogUtils.e("ReflectUtils", "[invoke] methodName=" + methodName
					+ " error:" + e.getMessage());
        }
        return null;
    }    
    
    public static <T> T invoke(Class<?> cls, Object obj, String methodName, Class<?>[] argClasses, Object[] args) {
        Method method = null;
        try {
            method = cls.getDeclaredMethod(methodName, argClasses);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        if (method == null) {
            Class<?> superCls = cls.getSuperclass();
            while(superCls != null && superCls != Object.class) {
                try {
                    method = superCls.getMethod(methodName, argClasses);
                } catch (Exception e) {
                } 
                superCls = superCls.getSuperclass();            
            }
        }
        if (method == null) {
            return null;
        }
        try {
            method.setAccessible(true);
            return (T)method.invoke(obj, args);
        } catch (Exception e) {
			LogUtils.e("ReflectUtils", "[invoke] methodName=" + methodName
					+ " error:" + e.getMessage());
        }
        return null;
    }
    
}
