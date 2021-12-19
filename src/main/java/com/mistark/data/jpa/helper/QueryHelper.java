package com.mistark.data.jpa.helper;

import com.mistark.data.jpa.meta.Query;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueryHelper {

    private final static Map<String, Class<? extends Expression>> KnownExpressions
            = new ConcurrentHashMap<String, Class<? extends Expression>>() {{
        put(Query.EQUAL, EqualsTo.class);
        put(Query.NOT_EQUAL, NotEqualsTo.class);
        put(Query.GREATER_THAN, GreaterThan.class);
        put(Query.GREATER_THAN_OR_EQUAL, GreaterThanEquals.class);
        put(Query.LESS_THAN, MinorThan.class);
        put(Query.LESS_THAN_OR_EQUAL, MinorThanEquals.class);
        put(Query.BETWEEN, Between.class);
        put(Query.NOT_BETWEEN, Between.class);
        put(Query.LIKE, LikeExpression.class);
        put(Query.LIKE_LEFT, LikeExpression.class);
        put(Query.LIKE_RIGHT, LikeExpression.class);
        put(Query.NOT_LIKE, LikeExpression.class);
        put(Query.IS_NULL, IsNullExpression.class);
        put(Query.NOT_NULL, IsNullExpression.class);
        put(Query.IN, InExpression.class);
        put(Query.NOT_IN, InExpression.class);
    }};

    private final static Map<String,String> LIKE_TPL = new ConcurrentHashMap<String, String>(){{
        put(Query.LIKE, "%%%s%%");
        put(Query.NOT_LIKE, "%%%s%%");
        put(Query.LIKE_LEFT, "%s%%");
        put(Query.LIKE_RIGHT, "%%%s");
    }};

    public static Class<? extends Expression> getExpression(String operator){
        return KnownExpressions.get(operator);
    }

    public static Class<? extends Expression> getExpression(Query.LogicOperator logicOperator){
        return KnownExpressions.get(logicOperator.name());
    }

    public static String getLikeValue(String operator, String value){
        return String.format(LIKE_TPL.get(operator), value);
    }

}
