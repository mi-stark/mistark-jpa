package com.mistark.data.jpa.meta;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class Query {

    public final static String EQUAL = "=";
    public final static String NOT_EQUAL = "!=";
    public final static String GREATER_THAN = ">";
    public final static String GREATER_THAN_OR_EQUAL = ">=";
    public final static String LESS_THAN = "<";
    public final static String LESS_THAN_OR_EQUAL = "<=";
    public final static String BETWEEN = "[]";
    public final static String NOT_BETWEEN = "![]";
    public final static String LIKE = "%";
    public final static String LIKE_LEFT = "*%";
    public final static String LIKE_RIGHT = "%*";
    public final static String NOT_LIKE = "!%";
    public final static String IS_NULL = "0";
    public final static String NOT_NULL = "!0";
    public final static String IN = "()";
    public final static String NOT_IN = "!()";

    @Getter
    private final boolean safeCheck;
    
    @Getter @Setter
    private Integer page = 1;
    @Getter @Setter
    private Integer pageSize = 10;
    @Getter @Setter
    private List<QueryCondition> conditions = new ArrayList<>();
    @Getter @Setter
    private List<SortField> sorts = new ArrayList<>();
    @Getter @Setter
    private QueryType queryType = QueryType.PAGE;

    public Query() {
        this.safeCheck = true;
    }

    public Query(boolean safeCheck) {
        this.safeCheck = safeCheck;
    }

    public Query page(int page){
        this.page = page;
        return this;
    }

    public Query pageSize(int size){
        this.pageSize = size;
        return this;
    }

    public Query equal(String field, Object value){
        conditions.add(new QueryCondition(field,value, EQUAL));
        return this;
    }

    public Query notEqual(String field, Object value){
        conditions.add(new QueryCondition(field,value, NOT_EQUAL));
        return this;
    }

    public Query greaterThan(String field, Object value){
        conditions.add(new QueryCondition(field,value, GREATER_THAN));
        return this;
    }

    public Query greaterThanOrEqual(String field, Object value){
        conditions.add(new QueryCondition(field,value, GREATER_THAN_OR_EQUAL));
        return this;
    }

    public Query lessThan(String field, Object value){
        conditions.add(new QueryCondition(field,value, LESS_THAN));
        return this;
    }

    public Query lessThanOrEqual(String field, Object value){
        conditions.add(new QueryCondition(field,value, LESS_THAN_OR_EQUAL));
        return this;
    }

    public Query between(String field, Object value){
        conditions.add(new QueryCondition(field,value, BETWEEN));
        return this;
    }

    public Query notBetween(String field, Object value){
        conditions.add(new QueryCondition(field,value, NOT_BETWEEN));
        return this;
    }

    public Query like(String field, Object value){
        conditions.add(new QueryCondition(field,value, LIKE));
        return this;
    }

    public Query likeLeft(String field, Object value){
        conditions.add(new QueryCondition(field,value, LIKE_LEFT));
        return this;
    }

    public Query likeRight(String field, Object value){
        conditions.add(new QueryCondition(field,value, LIKE_RIGHT));
        return this;
    }

    public Query notLike(String field, Object value){
        conditions.add(new QueryCondition(field,value, NOT_LIKE));
        return this;
    }

    public Query isNull(String field){
        conditions.add(new QueryCondition(field,null, IS_NULL));
        return this;
    }

    public Query notNull(String field){
        conditions.add(new QueryCondition(field,null, NOT_NULL));
        return this;
    }

    public Query in(String field, Collection<?> values){
        conditions.add(new QueryCondition(field,values, IN));
        return this;
    }

    public Query notIn(String field, Collection<?> values){
        conditions.add(new QueryCondition(field,values, NOT_IN));
        return this;
    }

    public Query and(){
        QueryCondition condition = conditions.get(conditions.size() - 1);
        if(condition != null){
            condition.setLogicOperator(LogicOperator.AND);
        }
        return this;
    }

    public Query or(){
        QueryCondition condition = conditions.get(conditions.size() - 1);
        if(condition != null){
            condition.setLogicOperator(LogicOperator.OR);
        }
        return this;
    }

    public Query parenthese(Consumer<List<QueryCondition>> action){
        List<QueryCondition> queryConditions = new ArrayList<>();
        action.accept(queryConditions);
        QueryCondition condition = new QueryCondition();
        condition.setConditions(queryConditions);
        conditions.add(condition);
        return this;
    }

    public Query orderBy(String field){
        SortField sortField = new SortField();
        sortField.setField(field);
        sorts.add(sortField);
        return this;
    }

    public Query asc(){
        SortField sortField = sorts.get(sorts.size() - 1);
        if(sortField != null){
            sortField.setOrder(SortOrder.ASC);
        }
        return this;
    }

    public Query desc(){
        SortField sortField = sorts.get(sorts.size() - 1);
        if(sortField != null){
            sortField.setOrder(SortOrder.DESC);
        }
        return this;
    }

    public Query total(){
        this.queryType = QueryType.TOTAL;
        return this;
    }

    public Query all(){
        this.queryType = QueryType.NORMAL;
        return this;
    }

    @Setter
    @Getter
    public static class QueryCondition{
        private String field;
        private Object value;
        private String operator = EQUAL;
        private LogicOperator logicOperator = LogicOperator.AND;
        private List<QueryCondition> conditions;

        public QueryCondition(){}

        public QueryCondition(String field, Object value, String operator) {
            this.field = field;
            this.value = value;
            this.operator = operator;
        }
    }

    @Setter
    @Getter
    public static class SortField{
        private String field;
        private SortOrder order = SortOrder.ASC;
    }

    public enum LogicOperator{
        AND,OR
    }

    public enum SortOrder {
        ASC,DESC
    }
    
    public enum QueryType {
        NORMAL,PAGE,TOTAL
    }

}
