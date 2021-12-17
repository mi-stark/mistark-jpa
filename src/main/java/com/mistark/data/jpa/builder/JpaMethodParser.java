package com.mistark.data.jpa.builder;


import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Method;

public abstract class JpaMethodParser {
    protected Configuration configuration;
    protected LanguageDriver languageDriver;
    protected MapperBuilderAssistant assistant;
    protected Class type;
    protected Method method;
    protected EntityMeta meta;

    public final void parse(Configuration configuration, MapperBuilderAssistant assistant, Class type, Method method){
        this.configuration = configuration;
        this.languageDriver = configuration.getDefaultScriptingLanguageInstance();
        this.assistant = assistant;
        this.type = type;
        this.method = method;
        this.meta = EntityHelper.fromMethod(type, method);
        buildStatement();
    }

    protected String getId(){
        return String.format("%s.%s", type.getName(), method.getName());
    }

    protected abstract void buildStatement();

    protected MappedStatement addMappedStatement(SqlSource sqlSource,
                                                 SqlCommandType sqlCommandType,
                                                 Class<?> parameterType,
                                                 String resultMap,
                                                 Class<?> resultType,
                                                 KeyGenerator keyGenerator,
                                                 String keyProperty,
                                                 String keyColumn) {
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
        return assistant.addMappedStatement(
                getId(),
                sqlSource,
                StatementType.PREPARED,
                sqlCommandType,
                null,
                null,
                null,
                parameterType,
                resultMap,
                resultType,
                null,
                !isSelect,
                isSelect,
                false,
                keyGenerator,
                keyProperty,
                keyColumn,
                null,
                languageDriver,
                null
        );
    }

}