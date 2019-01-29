package com.springboot.dubbo.auto.config;

import com.alibaba.dubbo.config.AbstractConfig;
import com.alibaba.dubbo.config.spring.context.properties.AbstractDubboConfigBinder;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;

import static org.springframework.boot.context.properties.source.ConfigurationPropertySources.from;

/**
 * @author liufei
 * @date 2019/1/29 15:00
 */
public class RelaxedDubboConfigBinder extends AbstractDubboConfigBinder {
    @Override
    public <C extends AbstractConfig> void bind(String prefix, C dubboConfig) {

        // Converts ConfigurationPropertySources
        Iterable<ConfigurationPropertySource> propertySources = from(getPropertySources());

        // Wrap Bindable from DubboConfig instance
        Bindable<C> bindable = Bindable.ofInstance(dubboConfig);

        Binder binder = new Binder(propertySources);

        // Get BindHandler
        BindHandler bindHandler = getBindHandler();

        // Bind
        binder.bind(prefix, bindable, bindHandler);

    }

    private BindHandler getBindHandler() {
        BindHandler handler = BindHandler.DEFAULT;
        if (isIgnoreInvalidFields()) {
            handler = new IgnoreErrorsBindHandler(handler);
        }
        if (!isIgnoreUnknownFields()) {
            UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
            handler = new NoUnboundElementsBindHandler(handler, filter);
        }
        return handler;
    }
}
