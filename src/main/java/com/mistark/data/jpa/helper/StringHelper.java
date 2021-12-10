package com.mistark.data.jpa.helper;

public class StringHelper {

    public static String toUnderline(String str){
        return str==null ? null: str.replaceAll("([a-z])([A-Z])", "$1_$2");
    }

}
