package com.mistark.data.jpa.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.helper.ParserHelper;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class Insert extends JpaMethodParser {

    private String TPL = "<script> INSERT INTO %s (%s) VALUES (%s) </script>";

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                meta.getTable(),
                ParserHelper.getInsertItems(meta),
                ParserHelper.getInsertValues(meta));
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, meta.getEntity());
        addMappedStatement(
                sqlSource,
                SqlCommandType.INSERT,
                meta.getEntity(),
                null,
                Integer.class,
                new NoKeyGenerator(),
                meta.getId().getName(),
                meta.getId().getColumn()
        );
    }
}
