package com.mistark.data.jpa.builder;

import com.mistark.data.jpa.annotation.BindParser;
import com.mistark.data.jpa.annotation.SoftDel;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.helper.SoftDelHelper;
import org.apache.ibatis.builder.BuilderException;
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

    public void parse(){
        if(CollectionUtils.isEmpty(methodParsers)) return;
        for (Method method : type.getMethods()) {
            if(method.isDefault()) continue;
            String id = getId(type, method);
            if(hasStatement(id)){
                MappedStatement ms = configuration.getMappedStatement(id);
                EntityHelper.fromStatement(ms);
                SoftDel softDel = AnnotationUtils.getAnnotation(method, SoftDel.class);
                if(softDel!=null){
                    SoftDelHelper.addSoftDelStatement(ms);
                }
                continue;
            }
            BindParser bindParser = method.getAnnotation(BindParser.class);
            JpaMethodParser parser;
            if(bindParser!=null){
                parser = methodParsers.get(bindParser.value().hashCode());
                if(parser == null){
                    throw new BuilderException(String.format("No corresponding method parser found for %s", bindParser.getClass().getName()));
                }
            }else {
                parser = methodParsers.get(method.getName().hashCode());
            }
            if(parser!=null) parser.parse(configuration, assistant, type, method);
        }
    }

    protected String getId(Class type, Method method){
        return String.format("%s.%s", type.getName(), method.getName());
    }

    protected boolean hasStatement(String id){
        return configuration.hasStatement(id);
    }
}
