package me.roboroads.robosort.util;

import java.util.*;

public class Util {
    public static <T> List<T> reverse(List<T> list) {
        List<T> reversedList = new ArrayList<>(list);
        Collections.reverse(reversedList);
        return reversedList;
    }
}
