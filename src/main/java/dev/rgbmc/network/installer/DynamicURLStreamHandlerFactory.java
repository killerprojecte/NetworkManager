package dev.rgbmc.network.installer;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class DynamicURLStreamHandlerFactory extends ParentAwareURLStreamHandlerFactory {

    protected static final ThreadLocal FACTORY = new InheritableThreadLocal();

    public static void push(URLStreamHandlerFactory factory) {
        if (!(factory instanceof ParentAwareURLStreamHandlerFactory)) {
            factory = new URLStreamHandlerFactoryWrapper(factory);
        }
        URLStreamHandlerFactory old = (URLStreamHandlerFactory) FACTORY.get();
        ((ParentAwareURLStreamHandlerFactory) factory).setParentFactory(old);
        FACTORY.set(factory);
    }

    public static void pop() {
        ParentAwareURLStreamHandlerFactory factory = (ParentAwareURLStreamHandlerFactory) FACTORY.get();
        if (factory != null) {
            FACTORY.set(factory.getParent());
        }
    }

    protected URLStreamHandler create(String protocol) {
        ParentAwareURLStreamHandlerFactory factory = (ParentAwareURLStreamHandlerFactory) FACTORY.get();
        if (factory != null) {
            return factory.createURLStreamHandler(protocol);
        }
        return null;
    }
}
