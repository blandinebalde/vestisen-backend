package com.vestisen.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActionLoggingInterceptor actionLoggingInterceptor;

    @Value("${file.upload-dir:uploads/images}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(actionLoggingInterceptor)
                .addPathPatterns("/api/**");
    }

    /** Servir les photos d'annonces : /annonce/** -> uploads/images/annonce/... */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!base.endsWith("/")) base += "/";
        registry.addResourceHandler("/annonce/**")
                .addResourceLocations(base + "annonce/");
    }
}
