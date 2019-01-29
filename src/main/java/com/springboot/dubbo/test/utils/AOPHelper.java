package com.springboot.dubbo.test.utils;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Field;

/**
 * @author liufei
 * @date 2019/1/29 10:53
 */
public class AOPHelper {
    public AOPHelper() {
    }

    public static Object getTarget(Object proxy) throws Exception {
        if (!AopUtils.isAopProxy(proxy)) {
            return proxy;
        } else {
            Object result = proxy;
            if (!AopUtils.isJdkDynamicProxy(proxy)) {
                result = getCglibProxyTargetObject(proxy);
            } else {
                while(AopUtils.isJdkDynamicProxy(result)) {
                    result = getJdkDynamicProxyTargetObject(result);
                }

                if (AopUtils.isCglibProxy(result)) {
                    result = getCglibProxyTargetObject(result);
                }
            }

            return result == null ? proxy : result;
        }
    }

    private static Object getCglibProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        Object target = ((AdvisedSupport)advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        return target;
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy)h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return ((AdvisedSupport)advised.get(aopProxy)).getTargetSource().getTarget();
    }
}
