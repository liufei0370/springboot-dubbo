package com.springboot.dubbo.test.spring;

import com.alibaba.dubbo.config.*;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.extension.SpringExtensionFactory;
import com.alibaba.dubbo.config.support.Parameter;
import com.springboot.dubbo.test.config.DubboReferenceConfig;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DubboReferenceBean<T> extends DubboReferenceConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {
    private static final long serialVersionUID = 213195494150089726L;
    private transient ApplicationContext applicationContext;

    public DubboReferenceBean() {
    }

    public DubboReferenceBean(Reference reference) {
        super(reference);
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    public Object getObject() throws Exception {
        return this.get();
    }

    public Class<?> getObjectType() {
        return this.getInterfaceClass();
    }

    @Parameter(
            excluded = true
    )
    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        Map monitorConfigMap;
        Iterator var3;
        if (this.getConsumer() == null) {
            monitorConfigMap = this.applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, ConsumerConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                ConsumerConfig consumerConfig = null;
                var3 = monitorConfigMap.values().iterator();

                label236:
                while(true) {
                    ConsumerConfig config;
                    do {
                        if (!var3.hasNext()) {
                            if (consumerConfig != null) {
                                this.setConsumer(consumerConfig);
                            }
                            break label236;
                        }

                        config = (ConsumerConfig)var3.next();
                    } while(config.isDefault() != null && !config.isDefault().booleanValue());

                    if (consumerConfig != null) {
                        throw new IllegalStateException("Duplicate consumer configs: " + consumerConfig + " and " + config);
                    }

                    consumerConfig = config;
                }
            }
        }

        if (this.getApplication() == null && (this.getConsumer() == null || this.getConsumer().getApplication() == null)) {
            monitorConfigMap = this.applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, ApplicationConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                ApplicationConfig applicationConfig = null;
                var3 = monitorConfigMap.values().iterator();

                label215:
                while(true) {
                    ApplicationConfig config;
                    do {
                        if (!var3.hasNext()) {
                            if (applicationConfig != null) {
                                this.setApplication(applicationConfig);
                            }
                            break label215;
                        }

                        config = (ApplicationConfig)var3.next();
                    } while(config.isDefault() != null && !config.isDefault().booleanValue());

                    if (applicationConfig != null) {
                        throw new IllegalStateException("Duplicate application configs: " + applicationConfig + " and " + config);
                    }

                    applicationConfig = config;
                }
            }
        }

        if (this.getModule() == null && (this.getConsumer() == null || this.getConsumer().getModule() == null)) {
            monitorConfigMap = this.applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, ModuleConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                ModuleConfig moduleConfig = null;
                var3 = monitorConfigMap.values().iterator();

                label194:
                while(true) {
                    ModuleConfig config;
                    do {
                        if (!var3.hasNext()) {
                            if (moduleConfig != null) {
                                this.setModule(moduleConfig);
                            }
                            break label194;
                        }

                        config = (ModuleConfig)var3.next();
                    } while(config.isDefault() != null && !config.isDefault().booleanValue());

                    if (moduleConfig != null) {
                        throw new IllegalStateException("Duplicate module configs: " + moduleConfig + " and " + config);
                    }

                    moduleConfig = config;
                }
            }
        }

        if ((this.getRegistries() == null || this.getRegistries().size() == 0) && (this.getConsumer() == null || this.getConsumer().getRegistries() == null || this.getConsumer().getRegistries().size() == 0) && (this.getApplication() == null || this.getApplication().getRegistries() == null || this.getApplication().getRegistries().size() == 0)) {
            monitorConfigMap = this.applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, RegistryConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                List<RegistryConfig> registryConfigs = new ArrayList();
                var3 = monitorConfigMap.values().iterator();

                label164:
                while(true) {
                    RegistryConfig config;
                    do {
                        if (!var3.hasNext()) {
                            if (registryConfigs != null && registryConfigs.size() > 0) {
                                super.setRegistries(registryConfigs);
                            }
                            break label164;
                        }

                        config = (RegistryConfig)var3.next();
                    } while(config.isDefault() != null && !config.isDefault().booleanValue());

                    registryConfigs.add(config);
                }
            }
        }

        if (this.getMonitor() == null && (this.getConsumer() == null || this.getConsumer().getMonitor() == null) && (this.getApplication() == null || this.getApplication().getMonitor() == null)) {
            monitorConfigMap = this.applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, MonitorConfig.class, false, false);
            if (monitorConfigMap != null && monitorConfigMap.size() > 0) {
                MonitorConfig monitorConfig = null;
                var3 = monitorConfigMap.values().iterator();

                label139:
                while(true) {
                    MonitorConfig config;
                    do {
                        if (!var3.hasNext()) {
                            if (monitorConfig != null) {
                                this.setMonitor(monitorConfig);
                            }
                            break label139;
                        }

                        config = (MonitorConfig)var3.next();
                    } while(config.isDefault() != null && !config.isDefault().booleanValue());

                    if (monitorConfig != null) {
                        throw new IllegalStateException("Duplicate monitor configs: " + monitorConfig + " and " + config);
                    }

                    monitorConfig = config;
                }
            }
        }

        this.checkAndLoadConfig();
        Boolean b = this.isInit();
        if (b == null && this.getConsumer() != null) {
            b = this.getConsumer().isInit();
        }

        if (b != null && b.booleanValue()) {
            this.getObject();
        }

    }
}
