package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UpdateById extends JpaMethodParser {

    private String TPL = "<script> UPDATE %s <set>%s</set> WHERE %s = #{%s} </script>";

    @Override
    public String getName() {
        return "updateById";
    }

    @Override
    protected void buildStatement() {
        String script = String.format(
                TPL,
                entityMeta.getTable(),
                entityMeta.getFields()
                        .values()
                        .stream()
                        .filter(f -> !isExclude(f))
                        .map(f -> getSetItem(f))
                        .collect(Collectors.joining()),
                entityMeta.getId().getColumn(),
                entityMeta.getId().getName());
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, entityMeta.getEntity());
        addMappedStatement(
                sqlSource,
                SqlCommandType.UPDATE,
                entityMeta.getEntity(),
                null,
                Integer.class,
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
    }

    private boolean isExclude(EntityField f){
        return !f.getTable().equals(EntityMeta.ALIAS)
                || f== entityMeta.getId()
                || f== entityMeta.getSoftDel();
    }

    private String getSetItem(EntityField f){
        return String.format("<if test=\"%s != null\">%s = #{%s},</if>", f.getName(), f.getColumn(), f.getName());
    }

}
