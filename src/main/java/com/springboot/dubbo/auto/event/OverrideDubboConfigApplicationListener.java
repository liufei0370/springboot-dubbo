package com.springboot.dubbo.auto.event;

import com.alibaba.dubbo.common.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.SortedMap;

import static com.springboot.dubbo.auto.utils.DubboUtils.*;

/**
 * @author liufei
 * @date 2019/1/29 15:06
 */
public class OverrideDubboConfigApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {

        /**
         * Gets Logger After LoggingSystem configuration ready
         * @see LoggingApplicationListener
         */
        final Logger logger = LoggerFactory.getLogger(getClass());

        ConfigurableEnvironment environment = event.getEnvironment();

        boolean override = environment.getProperty(OVERRIDE_CONFIG_PROPERTY_NAME, boolean.class,
                DEFAULT_OVERRIDE_CONFIG_PROPERTY_VALUE);

        if (override) {

            SortedMap<String, Object> dubboProperties = filterDubboProperties(environment);

            ConfigUtils.getProperties().putAll(dubboProperties);

            if (logger.isInfoEnabled()) {

                logger.info("Dubbo Config was overridden by externalized configuration {}", dubboProperties);

            }

        } else {

            if (logger.isInfoEnabled()) {

                logger.info("Disable override Dubbo Config caused by property {} = {}", OVERRIDE_CONFIG_PROPERTY_NAME, override);

            }

        }

    }
}
