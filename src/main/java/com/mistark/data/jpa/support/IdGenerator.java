package com.mistark.data.jpa.support;


import java.util.UUID;

public interface IdGenerator {

    long nextId();

    default String nextUUID(){
        return UUID.randomUUID().toString();
    }
}
