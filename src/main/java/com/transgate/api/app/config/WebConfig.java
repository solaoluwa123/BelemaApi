/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.app.config;

/**
 *
 * @author Olawoyin Samson
 */
import com.transgate.api.app.AuthTokenInterceptor;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 *
 * @author Ogunfowora Olufemi
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AuthTokenInterceptor authTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add the interceptor to all endpoints or specific paths
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/**");
    }
    
    
//    @Bean
//    public ErrorPageRegistrar errorPageRegistrar() {
//        return registry -> registry.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/index"));
//    }
}

//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/").setViewName(ViewNames.INDEX);
//    }
//    @Bean
//    public MultipartResolver multipartResolver() throws IOException {
//        CommonsMultipartResolver multipartResolver
//                = new CommonsMultipartResolver();
//        multipartResolver.setUploadTempDir(new FileSystemResource("/tmp/uploads"));
//        multipartResolver.setMaxUploadSize(2097152);
//        multipartResolver.setMaxInMemorySize(0);
//        return multipartResolver;
//    }
//}
