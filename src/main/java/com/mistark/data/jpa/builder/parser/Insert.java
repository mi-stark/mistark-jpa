package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.helper.SoftDelHelper;
import com.mistark.data.jpa.meta.EntityField;
import com.mistark.data.jpa.meta.EntityMeta;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Insert extends JpaMethodParser {

    private String TPL = "<script> INSERT INTO %s (%s) VALUES (%s) </script>";

    @Override
    public String getName() {
        return "insert";
    }

    @Override
    protected void buildStatement() {
        List<EntityField> fields = getFields();
        List<String> columns = fields.stream().map(f -> f.getColumn()).collect(Collectors.toList());
        List<String> values = fields.stream().map(f -> String.format("#{%s}", f.getName())).collect(Collectors.toList());
        if(SoftDelHelper.isSoftDelete(entityMeta)){
            EntityField softDelete = entityMeta.getSoftDel();
            columns.add(softDelete.getColumn());
            values.add(String.format("'%s'", SoftDelHelper.getValue(false, softDelete.getJavaType())));
        }
        String script = String.format(
                TPL,
                entityMeta.getTable(),
                columns.stream().collect(Collectors.joining(",")),
                values.stream().collect(Collectors.joining(",")));
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, entityMeta.getEntity());
        addMappedStatement(
                sqlSource,
                SqlCommandType.INSERT,
                entityMeta.getEntity(),
                null,
                Integer.class,
                new NoKeyGenerator(),
                entityMeta.getId().getName(),
                entityMeta.getId().getColumn()
        );
    }

    private List<EntityField> getFields(){
        return entityMeta.getFields()
                .values()
                .stream()
                .filter(f -> !isExclude(f)).collect(Collectors.toList());
    }

    private boolean isExclude(EntityField f){
        return !f.getTable().equals(EntityMeta.ALIAS) || f== entityMeta.getSoftDel();
    }
}
