package com.mistark.data.jpa.builder;

import com.mistark.data.jpa.annotation.BindParser;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
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
            if(parser!=null) parser.parse(configuration, assistant, method);
        }
    }
}
