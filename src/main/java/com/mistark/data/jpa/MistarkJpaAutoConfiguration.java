package com.mistark.data.jpa;

import com.mistark.data.jpa.binding.JpaMapperMethodFactory;
import com.mistark.data.jpa.binding.JpaMapperRegistry;
import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.plugin.PluginConfig;
import com.mistark.data.jpa.support.TenantIdService;
import com.mistark.data.jpa.support.UserIdService;
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
    private UserIdService userIdService;
    private TenantIdService tenantIdService;

    public MistarkJpaAutoConfiguration(ObjectProvider<List<JpaMapperMethodFactory>> methodFactoriesProvider,
                                       ObjectProvider<List<JpaMethodParser>> methodParsersProvider,
                                       ObjectProvider<UserIdService> userIdService,
                                       ObjectProvider<TenantIdService> tenantIdService) {
        this.jpaMethodFactories = methodFactoriesProvider.getIfAvailable();
        this.jpaMethodParsers = methodParsersProvider.getIfAvailable();
        this.userIdService = userIdService.getIfAvailable();
        this.tenantIdService = tenantIdService.getIfAvailable();
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

    @Bean
    public PluginConfig pluginConfig(){
        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.setUserIdService(this.userIdService);
        pluginConfig.setTenantIdService(this.tenantIdService);
        return pluginConfig;
    }

}
