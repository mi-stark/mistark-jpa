package com.mistark.data.jpa.binding;

import com.mistark.data.jpa.builder.JpaMapperBuilder;
import com.mistark.data.jpa.builder.JpaMethodParser;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JpaMapperRegistry extends MapperRegistry {

    private final Configuration config;
    private final Map<Class<?>, MapperProxyFactory<?>> showedMappers;
    private List<JpaMapperMethodFactory> methodFactories;
    private Map<Integer, JpaMethodParser> methodParsers;

    public JpaMapperRegistry(Configuration config) {
        super(config);
        MetaObject metaObject = SystemMetaObject.forObject(this);
        this.showedMappers = (Map<Class<?>, MapperProxyFactory<?>>)metaObject.getValue("knownMappers");
        this.config = config;
    }

    @Override
    public <T> void addMapper(Class<T> type) {
        if (type.isInterface()) {
            if (hasMapper(type)) {
                throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
            }
            boolean loadCompleted = false;
            try {
                JpaMapperProxyFactory factory = new JpaMapperProxyFactory(type);
                factory.setMethodFactories(methodFactories);
                showedMappers.put(type, factory);
                // It's important that the type is added before the parser is run
                // otherwise the binding may automatically be attempted by the
                // mapper parser. If the type is already known, it won't try.
                MapperAnnotationBuilder annotationBuilder = new MapperAnnotationBuilder(config, type);
                annotationBuilder.parse();

                // Custom builder
                JpaMapperBuilder jpaMapperBuilder = new JpaMapperBuilder(config, type, methodParsers);
                jpaMapperBuilder.parse();
                loadCompleted = true;
            } finally {
                if (!loadCompleted) {
                    showedMappers.remove(type);
                }
            }
        }
    }

    public void setMethodFactories(List<JpaMapperMethodFactory> methodFactories) {
        this.methodFactories = methodFactories;
    }

    public void setMethodParsers(List<JpaMethodParser> methodParsers) {
        this.methodParsers = new ConcurrentHashMap<>();
        if(CollectionUtils.isEmpty(methodParsers)) return;
        methodParsers.forEach(parser -> {
            this.methodParsers.put(parser.getClass().hashCode(), parser);
            this.methodParsers.put(parser.getClass().hashCode(), parser);
        });
    }
}
