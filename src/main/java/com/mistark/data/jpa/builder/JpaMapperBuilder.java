package com.mistark.data.jpa.builder;

import com.mistark.data.jpa.annotation.BindParser;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import lombok.SneakyThrows;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.Map;

public class JpaMapperBuilder {

    private final Configuration configuration;
    private final MapperBuilderAssistant assistant;
    private final Class<?> type;
    private final Map<Integer, JpaMethodParser> methodParsers;

    public JpaMapperBuilder(Configuration configuration, Class<?> type, Map<Integer, JpaMethodParser> methodParsers) {
        String resource = type.getName().replace('.', '/') + ".java (best guess)";
        this.assistant = new MapperBuilderAssistant(configuration, resource);
        this.assistant.setCurrentNamespace(type.getName());
        this.configuration = configuration;
        this.type = type;
        this.methodParsers = methodParsers;
    }

    @SneakyThrows
    public void parse() {
        if(CollectionUtils.isEmpty(methodParsers)) return;
        for (Method method : type.getMethods()) {
            if(method.isDefault()) continue;
            String id = getId(type, method);
            if(hasStatement(id)){
                MappedStatement ms = configuration.getMappedStatement(id);
                Class alternate = EntityHelper.getEntityFromStatement(ms);
                EntityMeta meta = EntityHelper.fromMethod(type, method, alternate);
                if(meta.isSoftDel()) {
                    SoftDelHelper.addSoftDelStatement(ms);
                }
                continue;
            }
            BindParser bindParser = AnnotationUtils.getAnnotation(method, BindParser.class);
            JpaMethodParser parser = bindParser!=null
                    ? bindParser.value().newInstance()
                    : methodParsers.get(method.getName().hashCode());
            if(parser==null) continue;
            parser.parse(configuration, assistant, type, method);
        }
    }

    protected String getId(Class type, Method method){
        return String.format("%s.%s", type.getName(), method.getName());
    }

    protected boolean hasStatement(String id){
        return configuration.hasStatement(id);
    }
}
