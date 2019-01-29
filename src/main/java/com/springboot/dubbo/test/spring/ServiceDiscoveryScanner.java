package com.springboot.dubbo.test.spring;

import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.config.annotation.Service;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author liufei
 * @date 2019/1/29 10:50
 */
public class ServiceDiscoveryScanner {
    private static final String SCANNER_CALSSNAME = "org.springframework.context.annotation.ClassPathBeanDefinitionScanner";
    private static final String ANNOTATION_FILTER_CLASSNAME = "org.springframework.core.type.filter.AnnotationTypeFilter";
    private static final String TYPE_FILTER_CLASSNAME = "org.springframework.core.type.filter.TypeFilter";
    private final BeanDefinitionRegistry beanDefinitionRegistry;
    public ServiceDiscoveryScanner(BeanDefinitionRegistry beanDefinitionRegistry) {
        this.beanDefinitionRegistry = beanDefinitionRegistry;
    }

    public void scan(String[] packages) throws Exception {
        try {
            Class<?> scannerClass = ReflectUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner");
            Constructor<?> ctor = scannerClass.getConstructor(BeanDefinitionRegistry.class, Boolean.TYPE);
            Object scanner = ctor.newInstance(this.beanDefinitionRegistry, true);
            Class<?> filterClass = ReflectUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter");
            Object filter = filterClass.getConstructor(Class.class).newInstance(Service.class);
            Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter", ReflectUtils.forName("org.springframework.core.type.filter.TypeFilter"));
            addIncludeFilter.invoke(scanner, filter);
            Method scan = scannerClass.getMethod("scan", String[].class);
            scan.invoke(scanner, packages);
        } catch (Throwable var9) {
            throw new Exception("scan扫描出错", var9);
        }
    }
}
