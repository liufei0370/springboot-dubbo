package com.springboot.dubbo.test.config;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.Version;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.AbstractReferenceConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.support.Parameter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.StaticContext;
import com.alibaba.dubbo.rpc.cluster.Cluster;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.support.ClusterUtils;
import com.alibaba.dubbo.rpc.protocol.injvm.InjvmProtocol;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DubboReferenceConfig<T> extends AbstractReferenceConfig {
    private static final long serialVersionUID = -5864351140409987595L;
    private static final Protocol refprotocol = (Protocol)ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private static final Cluster cluster = (Cluster)ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    private static final ProxyFactory proxyFactory = (ProxyFactory)ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    private String interfaceName;
    private Class<?> interfaceClass;
    private String client;
    private String url;
    private List<MethodConfig> methods;
    private ConsumerConfig consumer;
    private String protocol;
    private transient volatile T ref;
    private transient volatile Invoker<?> invoker;
    private transient volatile boolean initialized;
    private transient volatile boolean destroyed;
    private final List<URL> urls = new ArrayList();
    private final Object finalizerGuardian = new Object() {
        protected void finalize() throws Throwable {
            super.finalize();
            if (!DubboReferenceConfig.this.destroyed) {
                AbstractConfig.logger.warn("DubboReferenceConfig(" + DubboReferenceConfig.this.url + ") is not DESTROYED when FINALIZE");
            }

        }
    };
    private volatile Map<String, String> initedParameterMap;

    public DubboReferenceConfig() {
    }

    public DubboReferenceConfig(Reference reference) {
        this.appendAnnotation(Reference.class, reference);
    }

    public URL toUrl() {
        return this.urls != null && this.urls.size() != 0 ? (URL)this.urls.iterator().next() : null;
    }

    public List<URL> toUrls() {
        return this.urls;
    }

    public synchronized T get() {
        if (this.destroyed) {
            throw new IllegalStateException("Already destroyed!");
        } else {
            if (this.ref == null) {
                this.init();
            }

            return this.ref;
        }
    }

    public synchronized void destroy() {
        if (this.ref != null) {
            if (!this.destroyed) {
                this.destroyed = true;

                try {
                    this.invoker.destroy();
                } catch (Throwable var2) {
                    logger.warn("Unexpected err when destroy invoker of DubboReferenceConfig(" + this.url + ").", var2);
                }

                this.invoker = null;
                this.ref = null;
            }
        }
    }

    protected synchronized void checkAndLoadConfig() {
        if (this.initedParameterMap == null) {
            if (this.interfaceName != null && this.interfaceName.length() != 0) {
                if (StringUtils.isNotEmpty(DubboReferenceConfig.DefaultConfig.referenceURL) && !DubboReferenceConfig.DefaultConfig.distributedMode) {
                    this.setUrl(DubboReferenceConfig.DefaultConfig.referenceURL);
                }

                this.setTimeout(DubboReferenceConfig.DefaultConfig.timeout);
                this.checkDefault();
                appendProperties(this);
                if (this.getGeneric() == null && this.getConsumer() != null) {
                    this.setGeneric(this.getConsumer().getGeneric());
                }

                if (ProtocolUtils.isGeneric(this.getGeneric())) {
                    this.interfaceClass = GenericService.class;
                } else {
                    try {
                        this.interfaceClass = Class.forName(this.interfaceName, true, Thread.currentThread().getContextClassLoader());
                    } catch (ClassNotFoundException var18) {
                        throw new IllegalStateException(var18.getMessage(), var18);
                    }

                    this.checkInterfaceAndMethods(this.interfaceClass, this.methods);
                }

                String resolve = System.getProperty(this.interfaceName);
                String resolveFile = null;
                if (resolve == null || resolve.length() == 0) {
                    resolveFile = System.getProperty("dubbo.resolve.file");
                    if (resolveFile == null || resolveFile.length() == 0) {
                        File userResolveFile = new File(new File(System.getProperty("user.home")), "dubbo-resolve.properties");
                        if (userResolveFile.exists()) {
                            resolveFile = userResolveFile.getAbsolutePath();
                        }
                    }

                    if (resolveFile != null && resolveFile.length() > 0) {
                        Properties properties = new Properties();
                        FileInputStream fis = null;

                        try {
                            fis = new FileInputStream(new File(resolveFile));
                            properties.load(fis);
                        } catch (IOException var16) {
                            throw new IllegalStateException("Unload " + resolveFile + ", cause: " + var16.getMessage(), var16);
                        } finally {
                            try {
                                if (null != fis) {
                                    fis.close();
                                }
                            } catch (IOException var15) {
                                logger.warn(var15.getMessage(), var15);
                            }

                        }

                        resolve = properties.getProperty(this.interfaceName);
                    }
                }

                if (resolve != null && resolve.length() > 0) {
                    this.url = resolve;
                    if (logger.isWarnEnabled()) {
                        if (resolveFile != null && resolveFile.length() > 0) {
                            logger.warn("Using default dubbo resolve file " + resolveFile + " replace " + this.interfaceName + "" + resolve + " to p2p invoke remote service.");
                        } else {
                            logger.warn("Using -D" + this.interfaceName + "=" + resolve + " to p2p invoke remote service.");
                        }
                    }
                }

                if (this.consumer != null) {
                    if (this.application == null) {
                        this.application = this.consumer.getApplication();
                    }

                    if (this.module == null) {
                        this.module = this.consumer.getModule();
                    }

                    if (this.registries == null) {
                        this.registries = this.consumer.getRegistries();
                    }

                    if (this.monitor == null) {
                        this.monitor = this.consumer.getMonitor();
                    }
                }

                if (this.module != null) {
                    if (this.registries == null) {
                        this.registries = this.module.getRegistries();
                    }

                    if (this.monitor == null) {
                        this.monitor = this.module.getMonitor();
                    }
                }

                if (this.application != null) {
                    if (this.registries == null) {
                        this.registries = this.application.getRegistries();
                    }

                    if (this.monitor == null) {
                        this.monitor = this.application.getMonitor();
                    }
                }

                this.checkApplication();
                this.checkStubAndMock(this.interfaceClass);
                Map<String, String> map = new HashMap();
                Map<Object, Object> attributes = new HashMap();
                map.put("side", "consumer");
                map.put("dubbo", Version.getVersion());
                map.put("timestamp", String.valueOf(System.currentTimeMillis()));
                if (ConfigUtils.getPid() > 0) {
                    map.put("pid", String.valueOf(ConfigUtils.getPid()));
                }

                String prifix;
                if (!this.isGeneric().booleanValue()) {
                    prifix = Version.getVersion(this.interfaceClass, this.version);
                    if (prifix != null && prifix.length() > 0) {
                        map.put("revision", prifix);
                    }

                    map.put("methods", StringUtils.join(new HashSet(Arrays.asList(Wrapper.getWrapper(this.interfaceClass).getDeclaredMethodNames())), ","));
                }

                map.put("interface", this.interfaceName);
                appendParameters(map, this.application);
                appendParameters(map, this.module);
                appendParameters(map, this.consumer, "default");
                appendParameters(map, this);
                prifix = StringUtils.getServiceKey(map);
                if (this.methods != null && this.methods.size() > 0) {
                    Iterator var6 = this.methods.iterator();

                    while(var6.hasNext()) {
                        MethodConfig method = (MethodConfig)var6.next();
                        appendParameters(map, method, method.getName());
                        String retryKey = method.getName() + ".retry";
                        if (map.containsKey(retryKey)) {
                            String retryValue = (String)map.remove(retryKey);
                            if ("false".equals(retryValue)) {
                                map.put(method.getName() + ".retries", "0");
                            }
                        }

                        appendAttributes(attributes, method, prifix + "." + method.getName());
                        checkAndConvertImplicitConfig(method, map, attributes);
                    }
                }

                StaticContext.getSystemContext().putAll(attributes);
                this.initedParameterMap = map;
            } else {
                throw new IllegalStateException("<dubbo:reference interface=\"\" /> interface not allow null!");
            }
        }
    }

    private void init() {
        if (!this.initialized) {
            this.initialized = true;
            this.checkAndLoadConfig();
            this.ref = this.createProxy();
        }
    }

    private static void checkAndConvertImplicitConfig(MethodConfig method, Map<String, String> map, Map<Object, Object> attributes) {
        if (!Boolean.FALSE.equals(method.isReturn()) || method.getOnreturn() == null && method.getOnthrow() == null) {
            String onReturnMethodKey = StaticContext.getKey(map, method.getName(), "onreturn.method");
            Object onReturnMethod = attributes.get(onReturnMethodKey);
            if (onReturnMethod != null && onReturnMethod instanceof String) {
                attributes.put(onReturnMethodKey, getMethodByName(method.getOnreturn().getClass(), onReturnMethod.toString()));
            }

            String onThrowMethodKey = StaticContext.getKey(map, method.getName(), "onthrow.method");
            Object onThrowMethod = attributes.get(onThrowMethodKey);
            if (onThrowMethod != null && onThrowMethod instanceof String) {
                attributes.put(onThrowMethodKey, getMethodByName(method.getOnthrow().getClass(), onThrowMethod.toString()));
            }

            String onInvokeMethodKey = StaticContext.getKey(map, method.getName(), "oninvoke.method");
            Object onInvokeMethod = attributes.get(onInvokeMethodKey);
            if (onInvokeMethod != null && onInvokeMethod instanceof String) {
                attributes.put(onInvokeMethodKey, getMethodByName(method.getOninvoke().getClass(), onInvokeMethod.toString()));
            }

        } else {
            throw new IllegalStateException("method config error : return attribute must be set true when onreturn or onthrow has been setted.");
        }
    }

    private static Method getMethodByName(Class<?> clazz, String methodName) {
        try {
            return ReflectUtils.findMethodByMethodName(clazz, methodName);
        } catch (Exception var3) {
            throw new IllegalStateException(var3);
        }
    }

    private T createProxy() {
        URL tmpUrl = new URL("temp", "localhost", 0, this.initedParameterMap);
        boolean isJvmRefer;
        if (this.isInjvm() == null) {
            if (this.url != null && this.url.length() > 0) {
                isJvmRefer = false;
            } else if (InjvmProtocol.getInjvmProtocol().isInjvmRefer(tmpUrl)) {
                isJvmRefer = true;
            } else {
                isJvmRefer = false;
            }
        } else {
            isJvmRefer = this.isInjvm().booleanValue();
        }

        if (isJvmRefer) {
            URL url = (new URL("injvm", "127.0.0.1", 0, this.interfaceClass.getName())).addParameters(this.initedParameterMap);
            this.invoker = refprotocol.refer(this.interfaceClass, url);
            if (logger.isInfoEnabled()) {
                logger.info("Using injvm service " + this.interfaceClass.getName());
            }
        } else {
            if (this.url != null && this.url.length() > 0) {
                String[] us = Constants.SEMICOLON_SPLIT_PATTERN.split(this.url);
                if (us != null && us.length > 0) {
                    String[] var13 = us;
                    int var15 = us.length;

                    for(int var17 = 0; var17 < var15; ++var17) {
                        String u = var13[var17];
                        URL url = URL.valueOf(u);
                        if (url.getPath() == null || url.getPath().length() == 0) {
                            url = url.setPath(this.interfaceName);
                        }

                        if ("registry".equals(url.getProtocol())) {
                            this.urls.add(url.addParameterAndEncoded("refer", StringUtils.toQueryString(this.initedParameterMap)));
                        } else {
                            this.urls.add(ClusterUtils.mergeUrl(url, this.initedParameterMap));
                        }
                    }
                }
            } else {
                URL u;
                URL url;
                List<URL> us = this.loadRegistries(false);
                if (us != null && us.size() > 0) {
                    for(Iterator var4 = us.iterator(); var4.hasNext(); this.urls.add(u.addParameterAndEncoded("refer", StringUtils.toQueryString(this.initedParameterMap)))) {
                        u = (URL)var4.next();
                        url = this.loadMonitor(u);
                        if (url != null) {
                            this.initedParameterMap.put("monitor", URL.encode(url.toFullString()));
                        }
                    }
                }

                if (this.urls == null || this.urls.size() == 0) {
                    throw new IllegalStateException("No such any registry to reference " + this.interfaceName + " on the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion() + ", please config <dubbo:registry address=\"...\" /> to your spring config.");
                }
            }

            if (this.urls.size() == 1) {
                this.invoker = refprotocol.refer(this.interfaceClass, (URL)this.urls.get(0));
            } else {
                URL u;
                URL url;
                List<Invoker<?>> invokers = new ArrayList();
                URL registryURL = null;
                Iterator var16 = this.urls.iterator();

                while(var16.hasNext()) {
                    url = (URL)var16.next();
                    invokers.add(refprotocol.refer(this.interfaceClass, url));
                    if ("registry".equals(url.getProtocol())) {
                        registryURL = url;
                    }
                }

                if (registryURL != null) {
                    u = registryURL.addParameterIfAbsent("cluster", "available");
                    this.invoker = cluster.join(new StaticDirectory(u, invokers));
                } else {
                    this.invoker = cluster.join(new StaticDirectory(invokers));
                }
            }
        }

        Boolean c = this.check;
        if (c == null && this.consumer != null) {
            c = this.consumer.isCheck();
        }

        if (c == null) {
            c = true;
        }

        if (c.booleanValue() && !this.invoker.isAvailable()) {
            throw new IllegalStateException("Failed to check the status of the service " + this.interfaceName + ". No provider available for the service " + (this.group == null ? "" : this.group + "/") + this.interfaceName + (this.version == null ? "" : ":" + this.version) + " from the url " + this.invoker.getUrl() + " to the consumer " + NetUtils.getLocalHost() + " use dubbo version " + Version.getVersion());
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Refer dubbo service " + this.interfaceClass.getName() + " from url " + this.invoker.getUrl());
            }

            return (T) proxyFactory.getProxy(this.invoker);
        }
    }

    private void checkDefault() {
        if (this.consumer == null) {
            this.consumer = new ConsumerConfig();
        }

        appendProperties(this.consumer);
    }

    public Class<?> getInterfaceClass() {
        if (this.interfaceClass != null) {
            return this.interfaceClass;
        } else if (!this.isGeneric().booleanValue() && (this.getConsumer() == null || !this.getConsumer().isGeneric().booleanValue())) {
            try {
                if (this.interfaceName != null && this.interfaceName.length() > 0) {
                    this.interfaceClass = Class.forName(this.interfaceName, true, Thread.currentThread().getContextClassLoader());
                }
            } catch (ClassNotFoundException var2) {
                throw new IllegalStateException(var2.getMessage(), var2);
            }

            return this.interfaceClass;
        } else {
            return GenericService.class;
        }
    }

    /** @deprecated */
    @Deprecated
    public void setInterfaceClass(Class<?> interfaceClass) {
        this.setInterface(interfaceClass);
    }

    public String getInterface() {
        return this.interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
        if (this.id == null || this.id.length() == 0) {
            this.id = interfaceName;
        }

    }

    public void setInterface(Class<?> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        } else {
            this.interfaceClass = interfaceClass;
            this.setInterface(interfaceClass == null ? (String)null : interfaceClass.getName());
        }
    }

    public String getClient() {
        return this.client;
    }

    public void setClient(String client) {
        checkName("client", client);
        this.client = client;
    }

    @Parameter(
            excluded = true
    )
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<MethodConfig> getMethods() {
        return this.methods;
    }

    public void setMethods(List<MethodConfig> methods) {
        this.methods = methods;
    }

    public ConsumerConfig getConsumer() {
        return this.consumer;
    }

    public void setConsumer(ConsumerConfig consumer) {
        this.consumer = consumer;
    }

    public String getProtocol() {
        return this.protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    Invoker<?> getInvoker() {
        return this.invoker;
    }

    public static class DefaultConfig {
        public static String referenceURL = null;
        public static boolean distributedMode = false;
        public static int timeout = 10000;

        public DefaultConfig() {
        }
    }
}
