package com.mistark.data.jpa.plugin.dialect;


import com.mistark.data.jpa.plugin.dialect.page.*;
import com.mistark.meta.Value;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DialectHelper {

    private final static Map<String,Class<? extends PageDialect>> PAGE_DIALECTS = new HashMap<String,Class<? extends PageDialect>>(){{
        put("hsqldb", HsqldbPage.class);
        put("h2", HsqldbPage.class);
        put("postgresql", HsqldbPage.class);
        put("phoenix", HsqldbPage.class);
        put("mysql", MySqlPage.class);
        put("mariadb", MySqlPage.class);
        put("sqlite", MySqlPage.class);
        put("herddb", HerdDBPage.class);
        put("oracle", OraclePage.class);
        put("db2", Db2Page.class);
        put("dm", OraclePage.class);
        put("edb", OraclePage.class);
        put("oscar", MySqlPage.class);
    }};

    private final static Map<Integer, PageDialect> KnownDialects = new ConcurrentHashMap<>();

    public static PageDialect getPageDialect(DataSource dataSource){
        return KnownDialects.computeIfAbsent(dataSource.hashCode(), k -> {
            PageDialect pageDialect = null;
            Connection connection = null;
            try {
                connection = dataSource.getConnection();
                String url = connection.getMetaData().getURL().toLowerCase();
                Value<Class> type = new Value<>();
                PAGE_DIALECTS.entrySet().stream().anyMatch(entry -> {
                    if (url.contains(":" + entry.getKey().toLowerCase() + ":")) {
                        type.set(entry.getValue());
                        return true;
                    }
                    return false;
                });
                pageDialect = (PageDialect) type.get().newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
            }finally {
                if (connection!=null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return pageDialect;
        });
    }

}
