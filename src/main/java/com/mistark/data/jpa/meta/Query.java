package com.mistark.data.jpa.meta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mistark.data.jpa.annotation.SortType;
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
    private boolean safeCheck;
    
    @Getter @Setter
    private Integer page = 1;

    @Getter @Setter
    private Integer pageSize = 10;

    @Getter @Setter
    private List<QueryFilter> filters = new ArrayList<>();

    @Getter @Setter
    private List<QuerySort> sorters = new ArrayList<>();

    @JsonIgnore
    @Getter @Setter
    private QueryType queryType = QueryType.PAGE;

    public Query() {
        this.safeCheck = true;
    }

    public Query(boolean safeCheck) {
        this.safeCheck = safeCheck;
    }

    public Query(Query query){
        this.page = query.getPage();
        this.pageSize = query.getPageSize();
        this.filters = query.getFilters();
        this.sorters = query.getSorters();
        this.queryType = query.getQueryType();
        this.safeCheck = query.isSafeCheck();
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
        filters.add(new QueryFilter(field,value, EQUAL));
        return this;
    }

    public Query notEqual(String field, Object value){
        filters.add(new QueryFilter(field,value, NOT_EQUAL));
        return this;
    }

    public Query greaterThan(String field, Object value){
        filters.add(new QueryFilter(field,value, GREATER_THAN));
        return this;
    }

    public Query greaterThanOrEqual(String field, Object value){
        filters.add(new QueryFilter(field,value, GREATER_THAN_OR_EQUAL));
        return this;
    }

    public Query lessThan(String field, Object value){
        filters.add(new QueryFilter(field,value, LESS_THAN));
        return this;
    }

    public Query lessThanOrEqual(String field, Object value){
        filters.add(new QueryFilter(field,value, LESS_THAN_OR_EQUAL));
        return this;
    }

    public Query between(String field, Object value){
        filters.add(new QueryFilter(field,value, BETWEEN));
        return this;
    }

    public Query notBetween(String field, Object value){
        filters.add(new QueryFilter(field,value, NOT_BETWEEN));
        return this;
    }

    public Query like(String field, Object value){
        filters.add(new QueryFilter(field,value, LIKE));
        return this;
    }

    public Query likeLeft(String field, Object value){
        filters.add(new QueryFilter(field,value, LIKE_LEFT));
        return this;
    }

    public Query likeRight(String field, Object value){
        filters.add(new QueryFilter(field,value, LIKE_RIGHT));
        return this;
    }

    public Query notLike(String field, Object value){
        filters.add(new QueryFilter(field,value, NOT_LIKE));
        return this;
    }

    public Query isNull(String field){
        filters.add(new QueryFilter(field,null, IS_NULL));
        return this;
    }

    public Query notNull(String field){
        filters.add(new QueryFilter(field,null, NOT_NULL));
        return this;
    }

    public Query in(String field, Collection<?> values){
        filters.add(new QueryFilter(field,values, IN));
        return this;
    }

    public Query notIn(String field, Collection<?> values){
        filters.add(new QueryFilter(field,values, NOT_IN));
        return this;
    }

    public Query and(){
        QueryFilter condition = filters.get(filters.size() - 1);
        if(condition != null){
            condition.setLogicOperator(LogicOperator.AND);
        }
        return this;
    }

    public Query or(){
        QueryFilter condition = filters.get(filters.size() - 1);
        if(condition != null){
            condition.setLogicOperator(LogicOperator.OR);
        }
        return this;
    }

    public Query parenthese(Consumer<List<QueryFilter>> action){
        List<QueryFilter> queryFilters = new ArrayList<>();
        action.accept(queryFilters);
        QueryFilter condition = new QueryFilter();
        condition.setGroup(queryFilters);
        filters.add(condition);
        return this;
    }

    public Query orderBy(String field){
        QuerySort querySort = new QuerySort();
        querySort.setOrderBy(field);
        sorters.add(querySort);
        return this;
    }

    public Query orderBy(String field, SortType sortType){
        QuerySort querySort = new QuerySort();
        querySort.setOrderBy(field);
        querySort.setSortType(sortType);
        sorters.add(querySort);
        return this;
    }

    public Query asc(){
        QuerySort querySort = sorters.get(sorters.size() - 1);
        if(querySort != null){
            querySort.setSortType(SortType.ASC);
        }
        return this;
    }

    public Query desc(){
        QuerySort querySort = sorters.get(sorters.size() - 1);
        if(querySort != null){
            querySort.setSortType(SortType.DESC);
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

    public Query limit(int limit){
        this.queryType = QueryType.PAGE;
        this.page = 1;
        this.pageSize = limit;
        if(limit == 1){
            this.sorters = new ArrayList<>();
        }
        return this;
    }

    @Setter
    @Getter
    public static class QueryFilter{
        private String field;
        private Object value;
        private String operator = EQUAL;
        private LogicOperator logicOperator = LogicOperator.AND;
        private List<QueryFilter> group;

        public QueryFilter(){}

        public QueryFilter(String field, Object value, String operator) {
            this.field = field;
            this.value = value;
            this.operator = operator;
        }
    }

    @Setter
    @Getter
    public static class QuerySort{
        private String orderBy;
        private SortType sortType = SortType.ASC;
    }

    public enum LogicOperator{
        AND,OR
    }
    
    public enum QueryType {
        NORMAL,PAGE,TOTAL
    }

}
