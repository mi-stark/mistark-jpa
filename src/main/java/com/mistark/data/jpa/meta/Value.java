package com.mistark.data.jpa.meta;

public class Value<T> {

    private T value;

    public Value(){
        this.value = null;
    }

    public Value(T v){
        this.set(v);
    }

    public T get(){
        return this.value;
    }

    public void set(T v){
        this.value = v;
    }

    public boolean isArray(){
        return isArray(this.value);
    }

    public static boolean isArray(Object o){
        return o == null ? false : o.getClass().isArray();
    }
}
