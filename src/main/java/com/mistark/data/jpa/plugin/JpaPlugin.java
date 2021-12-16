package com.mistark.data.jpa.plugin;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

public interface JpaPlugin {
    boolean match(MappedStatement ms, BoundSql boundSql);
    void patch(MappedStatement ms, BoundSql boundSql) throws Throwable;
}
