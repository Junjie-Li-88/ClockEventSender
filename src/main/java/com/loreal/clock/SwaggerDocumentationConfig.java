package com.loreal.clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerDocumentationConfig {
    @Primary
    @Bean
    public SwaggerResourcesProvider swaggerResourcesProvider(
            InMemorySwaggerResourcesProvider defaultResourcesProvider) {
        /*获取resourceName*/
        return () -> {
            List<SwaggerResource> resources = new ArrayList<>();
            /*循环读取配置文件的资源名称（可配置多个）*/
            Arrays.asList("api1", "api2")
                    .forEach(resourceName -> resources.add(loadResource(resourceName)));
            return resources;
        };
    }
    /*调用配置文件*/
    private SwaggerResource loadResource(String resource) {
        SwaggerResource wsResource = new SwaggerResource();
        wsResource.setName(resource);
        wsResource.setSwaggerVersion("2.0");
        /*地址*/
        wsResource.setLocation("/swagger-apis/" + resource + "/swagger.yaml");
        return wsResource;
    }
}