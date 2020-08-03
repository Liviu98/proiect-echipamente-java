package com.echipamentespalatorii.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;


@Configuration
public class WebConfiguration implements WebMvcConfigurer {

   @Bean
   public MessageSource messageSource() {
       ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
      
       // incarcare proprietati validare
       messageSource.setBasename("classpath:validation");
      
       messageSource.setDefaultEncoding("UTF-8");
       
       return messageSource;
   }
 
}