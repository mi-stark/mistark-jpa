package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.builder.ParserHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class QueryList extends JpaMethodParser {

    private final String TPL = "<script> SELECT %s FROM %s %s %s </script>";

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                ParserHelper.getSelectItems(meta),
                meta.getTable(),
                EntityMeta.ALIAS,
                ParserHelper.getJoinItems(meta));
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, Object.class);
        addMappedStatement(
                sqlSource,
                SqlCommandType.SELECT,
                Object.class,
                null,
                meta.getEntity(),
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
    }

}
