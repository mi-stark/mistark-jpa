package com.mistark.data.jpa.helper;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

public class PluginHelper {

    public static MappedStatement getMappedStatement(StatementHandler statementHandler) {
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        return (MappedStatement) metaObject.getValue("delegate.mappedStatement");
    }

}
