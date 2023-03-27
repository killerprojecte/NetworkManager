package dev.rgbmc.reversenetx;

import dev.rgbmc.reversenetx.utils.DynamicURLStreamHandlerFactory;
import dev.rgbmc.reversenetx.utils.Installer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Hashtable;

public final class ReverseNetX extends JavaPlugin {
    private static URLStreamHandler http_origin;
    private static URLStreamHandler https_origin;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            new URL("http://.");
            new URL("https://.");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        try {
            Field handlersField = URL.class.getDeclaredField("handlers");
            //handlersField.setAccessible(true);
            Object handlersBase = Installer.unsafe.staticFieldBase(handlersField);
            long handlersOffset = Installer.unsafe.staticFieldOffset(handlersField);
            Hashtable hashtable = (Hashtable) Installer.unsafe.getObject(handlersBase, handlersOffset);
            http_origin = (URLStreamHandler) hashtable.get("http");
            https_origin = (URLStreamHandler) hashtable.get("https");
            //System.out.println(http_origin.getClass().getName());
            //System.out.println(https_origin.getClass().getName());
            URLStreamHandlerFactory dynamicFactory = new DynamicURLStreamHandlerFactory();
            Installer.setURLStreamHandlerFactory(dynamicFactory);
            DynamicURLStreamHandlerFactory.push(new URLStreamHandlerFactory() {
                @Override
                public URLStreamHandler createURLStreamHandler(String protocol) {
                    if (protocol.equals("http") || protocol.equals("https")) {
                        return new URLStreamHandler() {
                            @Override
                            protected URLConnection openConnection(URL u) throws IOException {
                                String url = u.toString();
                                System.out.println(url);
                                for (String key : getConfig().getConfigurationSection("reverse").getKeys(false)) {
                                    url = url.replace(key.replace("_dot_", "."), getConfig().getString("reverse." + key));
                                }
                                System.out.println(url);
                                try {
                                    if (url.startsWith("https")) {
                                        Method method = https_origin.getClass().getDeclaredMethod("openConnection", URL.class);
                                        method.setAccessible(true);
                                        return (URLConnection) method.invoke(https_origin, new URL(url));
                                    } else {
                                        Method method = http_origin.getClass().getDeclaredMethod("openConnection", URL.class);
                                        method.setAccessible(true);
                                        return (URLConnection) method.invoke(http_origin, new URL(url));
                                    }
                                } catch (Throwable e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        test();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DynamicURLStreamHandlerFactory.pop();
    }

    private void test() {
        try {
            URL url = new URL("http://ip-api.com/json/");
            URLConnection urlConnection = url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String str;
            while ((str = br.readLine()) != null) getLogger().info(str);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
