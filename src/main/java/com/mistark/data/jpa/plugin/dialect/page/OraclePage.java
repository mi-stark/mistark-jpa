package com.mistark.data.jpa.plugin.dialect.page;

import com.mistark.data.jpa.helper.SqlHelper;
import com.mistark.data.jpa.plugin.dialect.PageDialect;
import com.mistark.meta.Value;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.Map;

public class OraclePage extends PageDialect {

    private final String TPL = "SELECT * FROM ( SELECT TMP_PAGE.*, ROWNUM ROW_ID FROM M1 TMP_PAGE ) WHERE ROW_ID > \"1\" AND ROW_ID <= \"2\"";

    @Override
    public void patch(int page, int pageSize, Value<PlainSelect> selectValue, Map<String, Object> addon) throws Throwable {
        PlainSelect wrapSelect = SqlHelper.getPlainSelect(TPL);
        SubSelect fromItem = new SubSelect();
        fromItem.setSelectBody(selectValue.get());
        wrapSelect.getFromItem(SubSelect.class).getSelectBody(PlainSelect.class).setFromItem(fromItem);
        selectValue.set(wrapSelect);
        AndExpression and = wrapSelect.getWhere(AndExpression.class);
        String startKey = SqlHelper.getParamName();
        String endKey = SqlHelper.getParamName();
        and.getLeftExpression(GreaterThan.class).setRightExpression(SqlHelper.getMarkedExpression(startKey));
        and.getRightExpression(MinorThanEquals.class).setRightExpression(SqlHelper.getMarkedExpression(endKey));
        addon.put(startKey, offset(page, pageSize));
        addon.put(endKey, uplimit(page, pageSize));
    }
}