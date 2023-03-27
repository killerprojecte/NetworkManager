package dev.rgbmc.reversenetx.utils;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLStreamHandlerFactory;

public class Installer {

    public static MethodHandles.Lookup lookup;
    public static Unsafe unsafe;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            Field lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object lookupBase = unsafe.staticFieldBase(lookupField);
            long lookupOffset = unsafe.staticFieldOffset(lookupField);
            lookup = (MethodHandles.Lookup) unsafe.getObject(lookupBase, lookupOffset);
        } catch (Throwable ignored) {
        }

    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory factory)
            throws Exception {
        try {
            URL.setURLStreamHandlerFactory(factory);
        } catch (Error err) {
            final Field[] fields = URL.class.getDeclaredFields();
            int index = 0;
            Field factoryField = null;
            long offset = 0L;
            while (factoryField == null && index < fields.length) {
                final Field current = fields[index];
                if (Modifier.isStatic(current.getModifiers()) && current.getType().equals(URLStreamHandlerFactory.class)) {
                    factoryField = current;
                    offset = unsafe.staticFieldOffset(factoryField);
                } else {
                    index++;
                }
            }
            if (factoryField == null) {
                throw new Exception("Unable to detect static field in the URL class for the URLStreamHandlerFactory. Please report this error together with your exact environment to the Apache Excalibur project.");
            }
            try {
                URLStreamHandlerFactory oldFactory = (URLStreamHandlerFactory) unsafe.getObject(URL.class, offset);
                if (factory instanceof ParentAwareURLStreamHandlerFactory) {
                    ((ParentAwareURLStreamHandlerFactory) factory).setParentFactory(oldFactory);
                }
                unsafe.compareAndSwapObject(URL.class, offset, oldFactory, factory);
            } catch (IllegalArgumentException e) {
                throw new Exception("Unable to set url stream handler factory " + factory);
            }
        }
    }

    protected static Field getStaticURLStreamHandlerFactoryField() {
        Field[] fields = URL.class.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers()) && fields[i].getType().equals(URLStreamHandlerFactory.class)) {
                fields[i].setAccessible(true);
                return fields[i];
            }
        }
        return null;
    }
}
