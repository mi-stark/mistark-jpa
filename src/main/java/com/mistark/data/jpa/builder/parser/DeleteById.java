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
        boolean isSoft = SoftDelHelper.isSoftDel(meta);
        if(isSoft){
            script = String.format(
                    SOFT_DEL_TPL,
                    meta.getTable(),
                    meta.getSoftDel().getColumn(),
                    SoftDelHelper.getValue(true, meta.getSoftDel().getJavaType()),
                    meta.getId().getColumn(),
                    meta.getId().getName(),
                    meta.getSoftDel().getColumn(),
                    SoftDelHelper.getValue(false, meta.getSoftDel().getJavaType()));
        }else {
            script = String.format(
                    TPL,
                    meta.getTable(),
                    meta.getId().getColumn(),
                    meta.getId().getName());
        }
        SqlCommandType sqlCommandType = isSoft ? SqlCommandType.UPDATE : SqlCommandType.DELETE;
        SqlSource sqlSource = languageDriver.createSqlSource(configuration, script, meta.getEntity());
        MappedStatement ms = addMappedStatement(
                sqlSource,
                sqlCommandType,
                meta.getEntity(),
                null,
                Integer.class,
                NoKeyGenerator.INSTANCE,
                null,
                null
        );
        if(isSoft) SoftDelHelper.addSoftDelStatement(ms);
    }
}
