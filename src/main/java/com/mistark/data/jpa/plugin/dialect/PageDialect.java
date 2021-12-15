package com.mistark.data.jpa.plugin.dialect;


import com.mistark.meta.Value;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.Map;

public abstract class PageDialect {

    public abstract void patch(int page, int pageSize, Value<PlainSelect> selectValue, Map<String, Object> addon) throws Throwable;

    protected int offset(int page, int pageSize){
        return page > 0 ? (page - 1) * pageSize : 0;
    }

    protected int uplimit(int page, int pageSize){
        return offset(page,pageSize) + pageSize;
    }
}
