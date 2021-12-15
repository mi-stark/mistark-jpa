package com.mistark.data.jpa.plugin.dialect.page;

import com.mistark.data.jpa.helper.SqlHelper;
import com.mistark.data.jpa.plugin.dialect.PageDialect;
import com.mistark.meta.Value;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.Map;

public class MySqlPage extends PageDialect {

    @Override
    public void patch(int page, int pageSize, Value<PlainSelect> selectValue, Map<String, Object> addon) throws Throwable {
        String offsetKey = SqlHelper.getParamName();
        String pageSizeKey = SqlHelper.getParamName();
        Limit limit = new Limit();
        limit.setOffset(SqlHelper.getMarkedExpression(offsetKey));
        limit.setRowCount(SqlHelper.getMarkedExpression(pageSizeKey));
        selectValue.get().setLimit(limit);
        addon.put(offsetKey, offset(page, pageSize));
        addon.put(pageSizeKey, pageSize);
    }
}