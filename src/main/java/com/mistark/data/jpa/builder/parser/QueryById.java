package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.helper.EntityHelper;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.stream.Collectors;

@Component
public class QueryById extends JpaMethodParser {

    private final String TPL = "<script> SELECT %s FROM %s %s %s WHERE %s = #{%s}</script>";

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
                CollectionUtils.isEmpty(entityMeta.getJoins()) ? "" : entityMeta.getJoins()
                        .stream()
                        .map(j -> {
                            EntityMeta meta = EntityHelper.resolve(j.getEntity());
                            return String.format(
                                    "%s JOIN %s %s ON %s = %s",
                                    j.getJoinType(),
                                    meta.getTable(),
                                    j.getAlias(),
                                    j.getOnLeft(),
                                    j.getOnRight());
                        })
                        .collect(Collectors.joining(" ")),
                entityMeta.getId().getColumn(),
                entityMeta.getId().getName());

        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, Long.class);
        addMappedStatement(
                sqlSource,
                SqlCommandType.SELECT,
                null,
                null,
                entityMeta.getEntity(),
                new NoKeyGenerator(),
                entityMeta.getId().getName(),
                entityMeta.getId().getColumn()
        );
    }
}