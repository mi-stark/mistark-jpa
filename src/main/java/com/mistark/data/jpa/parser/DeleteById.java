package com.mistark.data.jpa.parser;

import com.mistark.data.jpa.annotation.Id;
import com.mistark.data.jpa.annotation.SoftDel;
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
    private String SOFT_DEL_TPL = "<script> UPDATE %s SET %s = #{%s} WHERE %s = #{%s} </script>";

    @Override
    protected void buildStatement() {
        String script;
        boolean isSoft = SoftDelHelper.isSoftDel(meta);
        if(isSoft){
            script = String.format(
                    SOFT_DEL_TPL,
                    meta.getTable(),
                    meta.annoFieldColumn(SoftDel.class),
                    meta.annoFieldName(SoftDel.class),
                    meta.annoFieldColumn(Id.class),
                    meta.annoFieldName(Id.class));
        }else {
            script = String.format(
                    TPL,
                    meta.getTable(),
                    meta.annoFieldColumn(Id.class),
                    meta.annoFieldName(Id.class));
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
