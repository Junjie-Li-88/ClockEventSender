//package com.loreal.clock;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;
//import springfox.documentation.swagger.web.SwaggerResource;
//import springfox.documentation.swagger.web.SwaggerResourcesProvider;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
////@Configuration
////public class SwaggerSpecConfig {
////
////    @Primary
////    @Bean
////    public SwaggerResourcesProvider swaggerResourcesProvider(
////            InMemorySwaggerResourcesProvider defaultResourcesProvider) {
////        return () -> {
////            List<SwaggerResource> resources = new ArrayList<>();
////            Arrays.asList("api1", "api2")
////                    .forEach(resourceName -> resources.add(loadResource(resourceName)));
////            return resources;
////        };
////    }
////
////    private SwaggerResource loadResource(String resource) {
////        SwaggerResource wsResource = new SwaggerResource();
////        wsResource.setName(resource);
////        wsResource.setSwaggerVersion("2.0");
////        wsResource.setLocation("/swagger.yml");
////        return wsResource;
////    }
////}
//@Configuration
//public class SwaggerSpecConfig {
//
//    @Primary
//    @Bean
//    public SwaggerResourcesProvider swaggerResourcesProvider(
//            InMemorySwaggerResourcesProvider defaultResourcesProvider) {
//        return () -> {
//            List<SwaggerResource> resources = new ArrayList<>();
//            Arrays.asList("api1", "api2")
//                    .forEach(resourceName -> resources.add(loadResource(resourceName)));
//            return resources;
//        };
//    }
//
//    private SwaggerResource loadResource(String resource) {
//        SwaggerResource wsResource = new SwaggerResource();
//        wsResource.setName(resource);
//        wsResource.setSwaggerVersion("2.0");
//        wsResource.setLocation("/swagger-apis/" + resource + "/swagger.yaml");
//        return wsResource;
//    }
//}