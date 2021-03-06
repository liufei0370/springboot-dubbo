package com.springboot.dubbo.auto.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author liufei
 * @date 2019/1/29 15:05
 */
public class AwaitingNonWebApplicationListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger logger = LoggerFactory.getLogger(AwaitingNonWebApplicationListener.class);

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean(false);

    private static final AtomicBoolean awaited = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        final SpringApplication springApplication = event.getSpringApplication();

        if (!WebApplicationType.NONE.equals(springApplication.getWebApplicationType())) {
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                synchronized (springApplication) {
                    if (logger.isInfoEnabled()) {
                        logger.info(" [Dubbo] Current Spring Boot Application is await...");
                    }
                    while (!awaited.get()) {
                        try {
                            springApplication.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        });

        // register ShutdownHook
        if (shutdownHookRegistered.compareAndSet(false, true)) {
            registerShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (springApplication) {
                        if (awaited.compareAndSet(false, true)) {
                            springApplication.notifyAll();
                            if (logger.isInfoEnabled()) {
                                logger.info(" [Dubbo] Current Spring Boot Application is about to shutdown...");
                            }
                            // Shutdown executorService
                            executorService.shutdown();
                        }
                    }
                }
            }));
        }
    }

    private void registerShutdownHook(Thread thread) {
        Runtime.getRuntime().addShutdownHook(thread);
    }
}
