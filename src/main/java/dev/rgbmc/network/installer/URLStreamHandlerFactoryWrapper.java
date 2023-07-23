package dev.rgbmc.network.installer;

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
