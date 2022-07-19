package com.loreal.clock;

import com.loreal.clock.Task.CallableSendHTTP;
import com.loreal.clock.Task.CallableTestSleep;
import com.microsoft.azure.kusto.data.exceptions.DataClientException;
import com.microsoft.azure.kusto.data.exceptions.DataServiceException;
import org.asynchttpclient.*;
import org.asynchttpclient.util.HttpConstants;
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
import static org.asynchttpclient.Dsl.post;

import java.awt.*;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.*;

import static org.asynchttpclient.Dsl.asyncHttpClient;

@EnableScheduling
@EnableWebMvc
@EnableAsync
@PropertySource(value = "classpath:application.properties", encoding = "utf-8")
@SpringBootApplication(scanBasePackages = {"com.loreal.clock"},exclude = {DataSourceAutoConfiguration.class})
public class EventWrapper implements AsyncUncaughtExceptionHandler, AsyncConfigurer {

    public static void main(String[] args) throws DataServiceException, URISyntaxException, DataClientException {
         ExecutorService pool = Executors.newFixedThreadPool(8000);

         for (int i = 0; i < 5000; i++) {
             Callable c = new CallableSendHTTP("http://127.0.0.1/echo", "{}", new HashMap<>(), null);
             Future f = pool.submit(c);
             System.out.println(i);
//             AsyncHttpClient asyncHttpClient = asyncHttpClient();
//             try {
//                 String body = "{}";
//                 Request request = post("http://127.0.0.1/echo").setBody(body).build();
//                 Future<Response> responseFuture = asyncHttpClient.executeRequest(request);
//                 //Response res = responseFuture.get(); // or handle asynchronously
//                 //System.out.println(res.getResponseBody());
//             } catch (Exception e) {
//                 e.printStackTrace();
//             }
         }
    }

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {

    }
}