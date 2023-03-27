package dev.rgbmc.reversenetx.utils;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class URLStreamHandlerFactoryWrapper extends ParentAwareURLStreamHandlerFactory {
    protected final URLStreamHandlerFactory wrapper;

    public URLStreamHandlerFactoryWrapper(URLStreamHandlerFactory f) {
        this.wrapper = f;
    }

    protected URLStreamHandler create(String protocol) {
        return this.wrapper.createURLStreamHandler(protocol);
    }


}
