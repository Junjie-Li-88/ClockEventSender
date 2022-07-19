package com.loreal.clock;

import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.DefaultManagedTaskExecutor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;

@EnableScheduling
@EnableWebMvc
@EnableAsync
@PropertySource(value = "classpath:application.properties", encoding = "utf-8")
@SpringBootApplication(scanBasePackages = {"com.loreal.clock"},exclude = {DataSourceAutoConfiguration.class})
public class ClockApplication implements AsyncUncaughtExceptionHandler, AsyncConfigurer {

    public static void main(String[] args) throws DataServiceException, URISyntaxException, DataClientException {
        try {
            new Main().updateConnStrings();
        } catch (Exception e) {
            System.out.println("error updating conn strings: " + e);
        }
        SpringApplication.run(ClockApplication.class, args);
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {

    }
}