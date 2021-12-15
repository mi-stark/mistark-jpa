package com.mistark.data.jpa.plugin.dialect.page;

import com.mistark.data.jpa.helper.SqlHelper;
import com.mistark.data.jpa.plugin.dialect.PageDialect;
import com.mistark.meta.Value;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.Map;

public class Db2Page extends PageDialect {

    private final String TPL = "SELECT * FROM (SELECT T1.*,ROWNUMBER() OVER() AS ROW_ID FROM M1 AS T1) T1 WHERE ROW_ID BETWEEN \"1\" AND \"2\"";

    @Override
    public void patch(int page, int pageSize, Value<PlainSelect> selectValue, Map<String, Object> addon) throws Throwable {
        PlainSelect wrapSelect = SqlHelper.getPlainSelect(TPL);
        SubSelect fromItem = new SubSelect();
        fromItem.setSelectBody(selectValue.get());
        wrapSelect.getFromItem(SubSelect.class).getSelectBody(PlainSelect.class).setFromItem(fromItem);
        selectValue.set(wrapSelect);
        Between between = (Between) wrapSelect.getWhere();
        String startKey = SqlHelper.getParamName();
        String endKey = SqlHelper.getParamName();
        between.setBetweenExpressionStart(SqlHelper.getMarkedExpression(startKey));
        between.setBetweenExpressionEnd(SqlHelper.getMarkedExpression(endKey));
        addon.put(startKey, offset(page, pageSize));
        addon.put(endKey, uplimit(page, pageSize));
    }

}