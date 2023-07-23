package dev.rgbmc.network.installer;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public abstract class ParentAwareURLStreamHandlerFactory implements URLStreamHandlerFactory {

    protected URLStreamHandlerFactory parentFactory;

    public void setParentFactory(URLStreamHandlerFactory factory) {
        this.parentFactory = factory;
    }

    public URLStreamHandlerFactory getParent() {
        return this.parentFactory;
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        URLStreamHandler handler = this.create(protocol);
        if (handler == null && this.parentFactory != null) {
            handler = this.parentFactory.createURLStreamHandler(protocol);
        }
        return handler;
    }

    protected abstract URLStreamHandler create(String protocol);
}
