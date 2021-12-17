package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.builder.ParserHelper;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class UpdateById extends JpaMethodParser {

    private String TPL = "<script> UPDATE %s <set>%s</set> WHERE %s = #{%s} </script>";

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                meta.getTable(),
                ParserHelper.getUpdateItems(meta),
                meta.getId().getColumn(),
                meta.getId().getName());
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, meta.getEntity());
        addMappedStatement(
                sqlSource,
                SqlCommandType.UPDATE,
                meta.getEntity(),
                null,
                Integer.class,
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
    }
}
