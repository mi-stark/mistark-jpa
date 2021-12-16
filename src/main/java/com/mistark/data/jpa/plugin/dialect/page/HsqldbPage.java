package com.mistark.data.jpa.plugin.dialect.page;

import com.mistark.data.jpa.helper.PluginHelper;
import com.mistark.data.jpa.plugin.dialect.PageDialect;
import com.mistark.meta.Value;
import lombok.Getter;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;

import java.util.Map;

public class HsqldbPage extends PageDialect {

    @Override
    public void patch(int page, int pageSize, Value<PlainSelect> selectValue, Map<String, Object> addon) throws Throwable {
        String offsetKey = PluginHelper.getParamName();
        String pageSizeKey = PluginHelper.getParamName();
        OffsetA offset = new OffsetA();
        offset.setOffsetA(PluginHelper.getMarkedExpression(offsetKey));
        Limit limit = new Limit();
        limit.setRowCount(PluginHelper.getMarkedExpression(pageSizeKey));
        selectValue.get().setOffset(offset);
        selectValue.get().setLimit(limit);
        addon.put(offsetKey, offset(page, pageSize));
        addon.put(pageSizeKey, pageSize);
    }

    @Getter
    @Setter
    class OffsetA extends Offset {
        Expression offsetA;
        public String toString() {
            return " OFFSET " + (offsetA!=null ? offsetA.toString() : "");
        }
    }
}