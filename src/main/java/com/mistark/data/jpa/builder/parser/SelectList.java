package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SelectList extends JpaMethodParser {

    private final String TPL = "<script> SELECT %s FROM %s %s %s</script>";

    @Override
    public String getName() {
        return "selectList";
    }

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                entityMeta.getFields()
                        .values()
                        .stream()
                        .map(f-> String.format("%s.%s AS %s", f.getTable(), f.getColumn(), f.getName()))
                        .collect(Collectors.joining(",")),
                entityMeta.getTable(),
                EntityMeta.ALIAS,
                entityMeta.getJoins()
                        .stream()
                        .map(j -> String.format(
                                "%s JOIN %s %s ON %s = %s",
                                j.getJoinType(),
                                j.getEntityMeta().getTable(),
                                j.getAlias(),
                                j.getOnLeft(),
                                j.getOnRight()))
                        .collect(Collectors.joining(" ")));
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, Object.class);
        addMappedStatement(
                sqlSource,
                SqlCommandType.SELECT,
                null,
                null,
                entityMeta.getEntity(),
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
    }
}
