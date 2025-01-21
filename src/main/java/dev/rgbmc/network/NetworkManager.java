package dev.rgbmc.network;

import dev.rgbmc.network.command.NetworkManagerCommand;
import dev.rgbmc.network.fastconfig.FastConfig;
import dev.rgbmc.network.fastconfig.Section;
import dev.rgbmc.network.installer.DynamicURLStreamHandlerFactory;
import dev.rgbmc.network.installer.Installer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

public final class NetworkManager extends JavaPlugin {
    private static final Map<String, URL> cachedURLs = new HashMap<>();
    private static final FastConfig config = new FastConfig();
    private static final Logger logger = Logger.getLogger("NetworkManager");
    public static NetworkManager instance;
    private static Map<String, URLStreamHandler> handlers = null;
    private static boolean DEBUG = false;
    private static boolean TRACE = false;

    static {
        saveFile("config.yml");
        config.refreshPoint(YamlConfiguration.loadConfiguration(new File(System.getProperty("user.dir") + "/plugins/NetworkManager/", "config.yml")));
        DEBUG = config.getBoolean("debug");
        TRACE = config.getBoolean("stacktrace");
        install();
    }

    private static void saveFile(String name) {
        saveFile(name, false, name);
    }

    private static void saveFile(String name, boolean replace, String saveName) {
        URL url = NetworkManager.class.getClassLoader().getResource(name);
        if (url == null) {
            logger.severe(name + " Not Found in JarFile");
            return;
        }
        File file = new File(System.getProperty("user.dir") + "/plugins/NetworkManager/" + saveName);
        if (!replace) {
            if (file.exists()) return;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        URLConnection connection = null;
        try {
            connection = url.openConnection();
        } catch (IOException e) {
            logger.severe("Failed unpack file " + name + ":" + e.getMessage());
        }
        connection.setUseCaches(false);
        try {
            saveFile(connection.getInputStream(), file);
        } catch (IOException e) {
            logger.severe("Failed unpack file " + name + ":" + e.getMessage());
        }
    }

    private static void saveFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    public static String getStreamContent(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private static void install() {
        logger.info("正在注入URL处理器");
        try {
            new URL("http://www.baidu.com").openConnection();
            new URL("https://www.baidu.com").openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            handlers = new HashMap<>(getHandlers());
            URLStreamHandlerFactory dynamicFactory = new DynamicURLStreamHandlerFactory();
            try {
                Installer.setURLStreamHandlerFactory(dynamicFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
            DynamicURLStreamHandlerFactory.push(protocol -> new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) {
                    String url = u.toString();
                    if (DEBUG) {
                        if (TRACE) {
                            new Exception("NetworkManager Debugger: " + url).printStackTrace();
                        }
                        logger.warning("正在请求: " + url);
                        logger.warning("格式化地址: " + url.replace(".", "_dot_"));
                    }
                    if (!handlers.containsKey(protocol)) {
                        try {
                            Hashtable<String, URLStreamHandler> upstreams = getHandlers();
                            URLStreamHandler upstream = upstreams.get(protocol);
                            if (upstream != null) {
                                handlers.put(protocol, upstream);
                                return getReversedURL(url, u, protocol);
                            }
                            return null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    try {
                        return getReversedURL(url, u, protocol);
                    } catch (MalformedURLException e) {
                        return null;
                    }
                }

                @Override
                protected URLConnection openConnection(URL u, Proxy p) throws IOException {
                    String url = u.toString();
                    if (DEBUG) {
                        new Exception("NetworkManager Debugger: " + url).printStackTrace();
                        logger.warning("正在请求: " + url);
                        logger.warning("格式化地址: " + url.replace(".", "_dot_"));
                    }
                    if (!handlers.containsKey(protocol)) {
                        try {
                            Hashtable<String, URLStreamHandler> upstreams = getHandlers();
                            URLStreamHandler upstream = upstreams.get(protocol);
                            if (upstream != null) {
                                handlers.put(protocol, upstream);
                                return getReversedURL(url, u, protocol, p);
                            }
                            return null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    try {
                        return getReversedURL(url, u, protocol, p);
                    } catch (MalformedURLException e) {
                        return null;
                    }
                }
            });
            logger.info("注入完成");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static URLConnection getReversedURL(String url, URL rUrl, String protocol) throws MalformedURLException {
        if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
            Section section = config.getSection("replace");
            for (String key : section.getKeys(false)) {
                String formatted = key.replace("_dot_", ".");
                if (url.startsWith(formatted)) {
                    return getReplacedContent(rUrl, section.getString(key), protocol);
                }
            }
        }
        rUrl = getReversedLink(url, rUrl);
        return getConnectionFromHandler(handlers.get(protocol), rUrl);
    }

    private static URLConnection getReversedURL(String url, URL rUrl, String protocol, Proxy proxy) throws MalformedURLException {
        if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
            Section section = config.getSection("replace");
            for (String key : section.getKeys(false)) {
                String formatted = key.replace("_dot_", ".");
                if (url.matches(formatted)) {
                    return getReplacedContent(rUrl, section.getString(key), protocol);
                }
            }
        }
        rUrl = getReversedLink(url, rUrl);
        return getConnectionFromHandler(handlers.get(protocol), rUrl, proxy);
    }

    private static URL getReversedLink(String url, URL rUrl) throws MalformedURLException {
        if (cachedURLs.containsKey(url)) {
            rUrl = cachedURLs.get(url);
        } else {
            Section section = config.getSection("reverse");
            for (String key : section.getKeys(false)) {
                String formatted = key.replace("_dot_", ".");
                if (url.matches(formatted)) {
                    String reversed = url.replaceAll(formatted, section.getString(key));
                    rUrl = new URL(reversed);
                    cachedURLs.put(url, rUrl);
                }
            }
            if (!cachedURLs.containsKey(url)) {
                cachedURLs.put(url, rUrl);
            }
        }
        if (DEBUG) {
            logger.warning("已被替换为: " + rUrl.toString());
        }
        return rUrl;
    }

    private static URLConnection getReplacedContent(URL url, String content, String protocol) throws MalformedURLException {
        if (protocol.equalsIgnoreCase("http")) {
            return new HttpURLConnection(url) {
                @Override
                public void disconnect() {

                }

                @Override
                public boolean usingProxy() {
                    return false;
                }

                @Override
                public void connect() throws IOException {
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    if (DEBUG) {
                        logger.info("地址: " + url.toString() + " 内容将被替换为: " + content);
                    }
                    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public OutputStream getOutputStream() throws IOException {
                    return new OutputStream() {
                        @Override
                        public void write(int b) throws IOException {

                        }
                    };
                }
            };
        }
        return new HttpsURLConnection(url) {
            @Override
            public String getCipherSuite() {
                return "";
            }

            @Override
            public Certificate[] getLocalCertificates() {
                return new Certificate[0];
            }

            @Override
            public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
                return new Certificate[0];
            }

            @Override
            public void disconnect() {

            }

            @Override
            public boolean usingProxy() {
                return false;
            }

            @Override
            public void connect() throws IOException {
                if (DEBUG) {
                    logger.info("地址: " + url.toString() + " 内容将被替换为: " + content);
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {

                    }
                };
            }
        };
    }

    private static void uninstall() {
        DynamicURLStreamHandlerFactory.pop();
    }

    private static URLConnection getConnectionFromHandler(URLStreamHandler handler, URL url) {
        try {
            Method method = handler.getClass().getDeclaredMethod("openConnection", URL.class);
            MethodHandle methodHandle = Installer.lookup.unreflect(method);
            URLConnection urlConnection = (URLConnection) methodHandle.invoke(handler, url);
            try {
                if (config.getBoolean("show-response")) {
                    logger.warning("地址: " + url.toString() + " 返回内容: " + getStreamContent(urlConnection.getInputStream()));
                }
            } catch (Exception ignored) {
            }
            return urlConnection;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static URLConnection getConnectionFromHandler(URLStreamHandler handler, URL url, Proxy proxy) {
        try {
            Method method = handler.getClass().getDeclaredMethod("openConnection", URL.class, Proxy.class);
            MethodHandle methodHandle = Installer.lookup.unreflect(method);
            URLConnection urlConnection = (URLConnection) methodHandle.invoke(handler, url, proxy);
            if (config.getBoolean("show-response")) {
                logger.warning("地址: " + url.toString() + " 返回内容: " + getStreamContent(urlConnection.getInputStream()));
            }
            return urlConnection;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Hashtable<String, URLStreamHandler> getHandlers() throws Exception {
        Field handlersField = URL.class.getDeclaredField("handlers");
        Object handlersBase = Installer.unsafe.staticFieldBase(handlersField);
        long handlersOffset = Installer.unsafe.staticFieldOffset(handlersField);
        return (Hashtable<String, URLStreamHandler>) Installer.unsafe.getObject(handlersBase, handlersOffset);
    }

    @Override
    public void onLoad() {
        instance = this;
        //saveDefaultConfig();
        reloadConfig();
        test();
    }

    @Override
    public void onEnable() {
        getCommand("networkmanager").setExecutor(new NetworkManagerCommand());
    }

    public void clearCache() {
        cachedURLs.clear();
    }

    @Override
    public void reloadConfig() {
        config.refreshPoint(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")));
        getLogger().info("NetworkManager 配置文件已重载");
        DEBUG = config.getBoolean("debug");
        TRACE = config.getBoolean("stacktrace");
        getLogger().info("DEBUG 状态: " + DEBUG);
    }

    @Override
    public void onDisable() {
        uninstall();
    }

    public FastConfig getFastConfig() {
        return config;
    }

    private void test() {
        try {
            URL url = new URL("https://www.baidu.com");
            url.openConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
