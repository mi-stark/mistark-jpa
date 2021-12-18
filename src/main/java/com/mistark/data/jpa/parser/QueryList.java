package com.mistark.data.jpa.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.helper.ParserHelper;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class QueryList extends JpaMethodParser {

    private final String TPL = "<script> SELECT %s FROM %s %s %s %s %s</script>";

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                ParserHelper.getSelectItems(meta),
                meta.getTable(),
                meta.getTableAlias(),
                ParserHelper.getJoinItems(meta),
                ParserHelper.getOrderByItems(meta),
                ParserHelper.getGroupByItems(meta));
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
