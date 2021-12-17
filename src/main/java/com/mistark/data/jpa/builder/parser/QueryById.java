package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.builder.ParserHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class QueryById extends JpaMethodParser {

    private final String TPL = "<script> SELECT %s FROM %s %s %s WHERE %s = #{%s} </script>";

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                ParserHelper.getSelectItems(meta),
                meta.getTable(),
                EntityMeta.ALIAS,
                ParserHelper.getJoinItems(meta),
                meta.getId().getColumn(),
                meta.getId().getName());
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, meta.getEntity());
        addMappedStatement(
                sqlSource,
                SqlCommandType.SELECT,
                meta.getEntity(),
                null,
                meta.getEntity(),
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
    }
}
