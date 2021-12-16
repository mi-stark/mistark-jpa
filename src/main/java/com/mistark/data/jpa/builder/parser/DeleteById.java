package com.mistark.data.jpa.builder.parser;

import com.mistark.data.jpa.builder.JpaMethodParser;
import com.mistark.data.jpa.helper.SoftDelHelper;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.springframework.stereotype.Component;

@Component
public class DeleteById extends JpaMethodParser {
    private String TPL = "<script> DELETE FROM %s WHERE %s = #{%s} </script>";
    private String SOFT_DEL_TPL = "<script> UPDATE %s SET %s = '%s' WHERE %s = #{%s} AND %s = '%s' </script>";

    @Override
    protected void buildStatement() {
        String script;
        boolean isSoft = SoftDelHelper.isSoftDel(entityMeta);
        if(isSoft){
            script = String.format(
                    SOFT_DEL_TPL,
                    entityMeta.getTable(),
                    entityMeta.getSoftDel().getColumn(),
                    SoftDelHelper.getValue(true, entityMeta.getSoftDel().getJavaType()),
                    entityMeta.getId().getColumn(),
                    entityMeta.getId().getName(),
                    entityMeta.getSoftDel().getColumn(),
                    SoftDelHelper.getValue(false, entityMeta.getSoftDel().getJavaType()));
        }else {
            script = String.format(
                    TPL,
                    entityMeta.getTable(),
                    entityMeta.getId().getColumn(),
                    entityMeta.getId().getName());
        }
        SqlCommandType sqlCommandType = isSoft ? SqlCommandType.UPDATE : SqlCommandType.DELETE;
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, entityMeta.getEntity());
        MappedStatement ms = addMappedStatement(
                sqlSource,
                sqlCommandType,
                entityMeta.getEntity(),
                null,
                Integer.class,
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
        if(isSoft) SoftDelHelper.addSoftDelStatement(ms);
    }
}
