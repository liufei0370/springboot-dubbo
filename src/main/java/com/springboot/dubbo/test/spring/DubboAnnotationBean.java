package com.springboot.dubbo.test.spring;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.springboot.dubbo.test.config.DubboReferenceConfig;
import com.springboot.dubbo.test.utils.AOPHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DubboAnnotationBean extends AbstractConfig implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {
    private static final long serialVersionUID = -7582802454287589552L;
    private static final Logger logger = LoggerFactory.getLogger(Logger.class);
    private ApplicationContext applicationContext;
    private String annotationPackage;
    private String[] annotationPackages;
    private final Set<ServiceConfig<?>> serviceConfigs = new ConcurrentHashSet();
    private final ConcurrentMap<String, DubboReferenceBean<?>> referenceConfigs = new ConcurrentHashMap();

    public DubboAnnotationBean() {
    }

    public String getPackage() {
        return this.annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = annotationPackage != null && annotationPackage.length() != 0 ? Constants.COMMA_SPLIT_PATTERN.split(annotationPackage) : null;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (!StringUtils.isBlank(this.annotationPackage) && beanFactory instanceof BeanDefinitionRegistry) {
            try {
                ServiceDiscoveryScanner scanner = new ServiceDiscoveryScanner((BeanDefinitionRegistry)beanFactory);
                scanner.scan(Constants.COMMA_SPLIT_PATTERN.split(this.annotationPackage));
            } catch (Throwable var3) {
                ;
            }
        }

    }

    public void destroy() throws Exception {
        Iterator var1 = this.serviceConfigs.iterator();

        while(var1.hasNext()) {
            ServiceConfig serviceConfig = (ServiceConfig)var1.next();

            try {
                serviceConfig.unexport();
            } catch (Throwable var5) {
                logger.error(var5.getMessage(), var5);
            }
        }

        var1 = this.referenceConfigs.values().iterator();

        while(var1.hasNext()) {
            DubboReferenceConfig referenceConfig = (DubboReferenceConfig)var1.next();

            try {
                referenceConfig.destroy();
            } catch (Throwable var4) {
                logger.error(var4.getMessage(), var4);
            }
        }

    }

    public Object postProcessAfterInitialization(Object originalBean, String beanName) throws BeansException {
        Object bean;
        try {
            bean = AOPHelper.getTarget(originalBean);
        } catch (Exception var13) {
            logger.error(var13);
            throw new RuntimeException(var13);
        }

        Service service = bean.getClass().getAnnotation(Service.class);
        if (!StringUtils.isBlank(this.getPackage()) && service != null && this.isMatchPackage(bean)) {
            if (service != null) {
                ServiceBean<Object> serviceConfig = new ServiceBean(service);
                if (Void.TYPE.equals(service.interfaceClass()) && "".equals(service.interfaceName())) {
                    if (bean.getClass().getInterfaces().length <= 0) {
                        throw new IllegalStateException("Failed to export remote service class " + bean.getClass().getName() + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                    }

                    serviceConfig.setInterface(bean.getClass().getInterfaces()[0]);
                }

                if (this.applicationContext != null) {
                    serviceConfig.setApplicationContext(this.applicationContext);
                    ArrayList protocolConfigs;
                    String[] var7;
                    int var8;
                    int var9;
                    String protocolId;
                    if (service.registry() != null && service.registry().length > 0) {
                        protocolConfigs = new ArrayList();
                        var7 = service.registry();
                        var8 = var7.length;

                        for(var9 = 0; var9 < var8; ++var9) {
                            protocolId = var7[var9];
                            if (protocolId != null && protocolId.length() > 0) {
                                protocolConfigs.add(this.applicationContext.getBean(protocolId, RegistryConfig.class));
                            }
                        }

                        serviceConfig.setRegistries(protocolConfigs);
                    } else {
                        serviceConfig.setRegistry(this.applicationContext.getBean(RegistryConfig.class));
                    }

                    if (service.provider() != null && service.provider().length() > 0) {
                        serviceConfig.setProvider(this.applicationContext.getBean(service.provider(), ProviderConfig.class));
                    }

                    if (service.monitor() != null && service.monitor().length() > 0) {
                        serviceConfig.setMonitor(this.applicationContext.getBean(service.monitor(), MonitorConfig.class));
                    }

                    if (service.application() != null && service.application().length() > 0) {
                        serviceConfig.setApplication(this.applicationContext.getBean(service.application(), ApplicationConfig.class));
                    }

                    if (service.module() != null && service.module().length() > 0) {
                        serviceConfig.setModule(this.applicationContext.getBean(service.module(), ModuleConfig.class));
                    }

                    if (service.provider() != null && service.provider().length() > 0) {
                        serviceConfig.setProvider(this.applicationContext.getBean(service.provider(), ProviderConfig.class));
                    }

                    if (service.protocol() != null && service.protocol().length > 0) {
                        protocolConfigs = new ArrayList();
                        var7 = service.registry();
                        var8 = var7.length;

                        for(var9 = 0; var9 < var8; ++var9) {
                            protocolId = var7[var9];
                            if (protocolId != null && protocolId.length() > 0) {
                                protocolConfigs.add(this.applicationContext.getBean(protocolId, ProtocolConfig.class));
                            }
                        }

                        serviceConfig.setProtocols(protocolConfigs);
                    }

                    try {
                        serviceConfig.afterPropertiesSet();
                    } catch (RuntimeException var11) {
                        throw var11;
                    } catch (Exception var12) {
                        throw new IllegalStateException(var12.getMessage(), var12);
                    }
                }

                serviceConfig.setRef(originalBean);
                this.serviceConfigs.add(serviceConfig);
                serviceConfig.export();
            }

            return originalBean;
        } else {
            return originalBean;
        }
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!this.isMatchPackage(bean)) {
            return bean;
        } else {
            Method[] methods = bean.getClass().getMethods();
            Method[] var4 = methods;
            int var5 = methods.length;

            int var6;
            Reference reference;
            Object value;
            for(var6 = 0; var6 < var5; ++var6) {
                Method method = var4[var6];
                String name = method.getName();
                if (name.length() > 3 && name.startsWith("set") && method.getParameterTypes().length == 1 && Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                    try {
                        reference = method.getAnnotation(Reference.class);
                        if (reference != null) {
                            value = this.refer(reference, method.getParameterTypes()[0]);
                            if (value != null) {
                                method.invoke(bean);
                            }
                        }
                    } catch (Throwable var12) {
                        logger.error("Failed to init remote service reference at method " + name + " in class " + bean.getClass().getName() + ", cause: " + var12.getMessage(), var12);
                    }
                }
            }

            Field[] fields = bean.getClass().getDeclaredFields();
            Field[] var14 = fields;
            var6 = fields.length;

            for(int var15 = 0; var15 < var6; ++var15) {
                Field field = var14[var15];

                try {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }

                    reference = field.getAnnotation(Reference.class);
                    if (reference != null) {
                        value = this.refer(reference, field.getType());
                        if (value != null) {
                            field.set(bean, value);
                        }
                    }
                } catch (Throwable var11) {
                    logger.error("Failed to init remote service reference at filed " + field.getName() + " in class " + bean.getClass().getName() + ", cause: " + var11.getMessage(), var11);
                }
            }

            return bean;
        }
    }

    private Object refer(Reference reference, Class<?> referenceClass) {
        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!Void.TYPE.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else {
            if (!referenceClass.isInterface()) {
                throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type " + referenceClass.getName() + " is not a interface.");
            }

            interfaceName = referenceClass.getName();
        }

        String key = reference.group() + "/" + interfaceName + ":" + reference.version();
        DubboReferenceBean<?> referenceConfig = this.referenceConfigs.get(key);
        if (referenceConfig == null) {
            referenceConfig = new DubboReferenceBean(reference);
            if (Void.TYPE.equals(reference.interfaceClass()) && "".equals(reference.interfaceName()) && referenceClass.isInterface()) {
                referenceConfig.setInterface(referenceClass);
            }

            if (this.applicationContext != null) {
                referenceConfig.setApplicationContext(this.applicationContext);
                if (reference.registry() != null && reference.registry().length > 0) {
                    List<RegistryConfig> registryConfigs = new ArrayList();
                    String[] var7 = reference.registry();
                    int var8 = var7.length;

                    for(int var9 = 0; var9 < var8; ++var9) {
                        String registryId = var7[var9];
                        if (registryId != null && registryId.length() > 0) {
                            registryConfigs.add(this.applicationContext.getBean(registryId, RegistryConfig.class));
                        }
                    }

                    referenceConfig.setRegistries(registryConfigs);
                }

                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer(this.applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }

                if (reference.monitor() != null && reference.monitor().length() > 0) {
                    referenceConfig.setMonitor(this.applicationContext.getBean(reference.monitor(), MonitorConfig.class));
                }

                if (reference.application() != null && reference.application().length() > 0) {
                    referenceConfig.setApplication(this.applicationContext.getBean(reference.application(), ApplicationConfig.class));
                }

                if (reference.module() != null && reference.module().length() > 0) {
                    referenceConfig.setModule(this.applicationContext.getBean(reference.module(), ModuleConfig.class));
                }

                if (reference.consumer() != null && reference.consumer().length() > 0) {
                    referenceConfig.setConsumer(this.applicationContext.getBean(reference.consumer(), ConsumerConfig.class));
                }

                try {
                    referenceConfig.afterPropertiesSet();
                } catch (RuntimeException var11) {
                    throw var11;
                } catch (Exception var12) {
                    throw new IllegalStateException(var12.getMessage(), var12);
                }
            }

            this.referenceConfigs.putIfAbsent(key, referenceConfig);
            referenceConfig = this.referenceConfigs.get(key);
        }

        return referenceConfig.get();
    }

    private boolean isMatchPackage(Object bean) {
        if (this.annotationPackages != null && this.annotationPackages.length != 0) {
            String beanClassName = bean.getClass().getName();
            String[] var3 = this.annotationPackages;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String pkg = var3[var5];
                if (beanClassName.startsWith(pkg)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}
