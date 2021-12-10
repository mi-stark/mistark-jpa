package com.mistark.data.jpa;

import com.mistark.data.jpa.binding.JpaMapperMethodFactory;
import com.mistark.data.jpa.binding.JpaMapperRegistry;
import com.mistark.data.jpa.builder.JpaMethodParser;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ComponentScan("com.mistark.data.jpa.*")
public class MistarkJpaAutoConfiguration {

    private List<JpaMapperMethodFactory> jpaMethodFactories;
    private List<JpaMethodParser> jpaMethodParsers;

    public MistarkJpaAutoConfiguration(ObjectProvider<List<JpaMapperMethodFactory>> methodFactoriesProvider,
                                       ObjectProvider<List<JpaMethodParser>> methodParsersProvider) {
        this.jpaMethodFactories = methodFactoriesProvider.getIfAvailable();
        this.jpaMethodParsers = methodParsersProvider.getIfAvailable();
    }

    @Bean
    public ConfigurationCustomizer configurationCustomizer(){
        return (org.apache.ibatis.session.Configuration configuration) -> {
            JpaMapperRegistry jpaMapperRegistry = new JpaMapperRegistry(configuration);
            jpaMapperRegistry.setMethodFactories(jpaMethodFactories);
            jpaMapperRegistry.setMethodParsers(jpaMethodParsers);
            MetaObject metaObject = SystemMetaObject.forObject(configuration);
            metaObject.setValue("mapperRegistry", jpaMapperRegistry);
        };
    }

}
