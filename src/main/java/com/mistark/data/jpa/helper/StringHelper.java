package com.mistark.data.jpa.helper;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringHelper {

    public static String toUnderline(String str){
        return str==null ? null: str.replaceAll("([a-z])([A-Z])", "$1_$2");
    }

    public static String doted(String...str){
        return Arrays.stream(str).collect(Collectors.joining("."));
    }

}
