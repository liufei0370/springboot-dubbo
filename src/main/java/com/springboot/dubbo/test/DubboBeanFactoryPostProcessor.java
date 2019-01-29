package com.springboot.dubbo.test;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.springboot.dubbo.test.config.DubboReferenceConfig.DefaultConfig;
import com.springboot.dubbo.test.spring.DubboAnnotationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author liufei
 * @date 2019/1/29 10:15
 */
public class DubboBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DubboBeanFactoryPostProcessor.class);
    private static final String DEFAULT_DUBBO_MODE = "provider";
    private static final String DEFAULT_PROTOCOL = "dubbo";
    private static final String DEFAULT_PORT = "20880";
    private static final String DEFAULT_TIMEOUT = "10000";
    private static final String DEFAULT_PACKAGE = "com.cmos.dubbo.service";
    private static final String DIRECT_MODE = "N/A";
    private static final String[] KEYS = new String[]{"port", "timeout", "registry-address", "reference-url", "application-name", "annotation-package"};
    private static final String PREFIX = "dubbo";
    private static final String DEFAULT_REFERENCE_URL = "dubbo://localhost:20880";
    private final ConfigurableEnvironment environment;
    private final ApplicationContext applicationContext;

    public DubboBeanFactoryPostProcessor(ApplicationContext applicationContext, ConfigurableEnvironment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (this.environment.containsProperty("dubbo.mode")) {
            String mode = this.getMode();
            if (!"provider".equals(mode) && !"consumer".equals(mode)) {
                throw new BeanInitializationException("无效Dubbo模式: " + this.getMode());
            }

            LOGGER.info("模式: " + this.getMode() + " 注册Dubbo配置和注解驱动，配置信息如下");
            String[] var3 = KEYS;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String key = var3[var5];
                LOGGER.info("dubbo." + this.getMode() + "." + key + " = " + this.getProperty(key, ""));
            }

            String registryAddress = this.getProperty("registry-address", "N/A");
            if (!"N/A".equalsIgnoreCase(registryAddress) && !StringUtils.isNotEmpty(registryAddress)) {
                DefaultConfig.distributedMode = true;
            }

            registry.registerBeanDefinition("dubbo-applicationConfig", this.createApplicationConfigDef());
            registry.registerBeanDefinition("dubbo-registryConfig", this.createRegistryConfigDef());
            if ("provider".equalsIgnoreCase(this.getMode())) {
                registry.registerBeanDefinition("dubbo-protocolConfig", this.createProtocolConfigDef());
            } else {
                if (this.containsProperty("reference-url")) {
                    DefaultConfig.referenceURL = this.getProperty("reference-url", "dubbo://localhost:20880");
                }

                DefaultConfig.timeout = Integer.parseInt(this.getProperty("timeout", "10000"));
            }

            registry.registerBeanDefinition("dubbo-annotationBean", this.createAnnotationBeanDef());
        }

    }

    private BeanDefinition createAnnotationBeanDef() {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DubboAnnotationBean.class);
        String packages = this.getProperty("annotation-package", "com.cmos.dubbo.service");
        beanDefinitionBuilder.addPropertyValue("package", packages);
        beanDefinitionBuilder.addPropertyValue("applicationContext", this.applicationContext);
        return beanDefinitionBuilder.getBeanDefinition();
    }

    private BeanDefinition createRegistryConfigDef() {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RegistryConfig.class);
        String address = this.getProperty("registry-address", "N/A");
        if (StringUtils.isBlank(address)) {
            beanDefinitionBuilder.addPropertyValue("address", "N/A");
        } else {
            beanDefinitionBuilder.addPropertyValue("address", address);
        }

        beanDefinitionBuilder.addPropertyValue("timeout", this.getProperty("timeout", "10000"));
        if (this.containsProperty("protocol")) {
            beanDefinitionBuilder.addPropertyValue("protocol", this.getProperty("protocol", "dubbo"));
        }

        return beanDefinitionBuilder.getBeanDefinition();
    }

    private BeanDefinition createProtocolConfigDef() {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ProtocolConfig.class);
        String port = this.getProperty("port", "20880");
        beanDefinitionBuilder.addPropertyValue("name", this.getProperty("protocol", "dubbo"));
        beanDefinitionBuilder.addPropertyValue("port", StringUtils.isNumeric(port) ? port : "20880");
        return beanDefinitionBuilder.getBeanDefinition();
    }

    private BeanDefinition createApplicationConfigDef() {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ApplicationConfig.class);
        beanDefinitionBuilder.addPropertyValue("name", this.getProperty("application-name", "dubbo-" + this.getMode()));
        return beanDefinitionBuilder.getBeanDefinition();
    }

    private String getMode() {
        return this.environment.getProperty("dubbo.mode", "provider").trim();
    }

    private String getProperty(String key, String defaultValue) {
        key = "dubbo." + this.getMode() + "." + key;
        return this.environment.getProperty(key, defaultValue);
    }

    private boolean containsProperty(String key) {
        key = "dubbo." + this.getMode() + "." + key;
        return this.environment.containsProperty(key);
    }

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
