package com.springboot.dubbo.test;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @author liufei
 * @date 2019/1/29 10:59
 */
public class DubboRegistryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public DubboRegistryInitializer() {
    }

    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        applicationContext.addBeanFactoryPostProcessor(new DubboBeanFactoryPostProcessor(applicationContext, environment));
    }
}
