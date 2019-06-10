package com.ericdmartell.maga.utils;

import org.apache.commons.lang.ObjectUtils;

import java.util.List;

public class HashUtil {
    public static <T> int hashObjectList(List<T> list) {
        int result = 1;

        for (T element : list) {
            int elementHash = element.hashCode();
            result = 31 * result + elementHash;
        }

        return result;
    }

    public static int hashLongList(List<Long> list) {
        int result = 1;

        for (Long element : list) {
            int elementHash = (int)(element ^ (element >>> 32));
            result = 31 * result + elementHash;
        }

        return result;
    }

    public static boolean arrayValueEquals(Object[] a, Object[] b) {
        if (a==null || b==null)
            return false;

        int length = a.length;
        if (b.length != length)
            return false;

        for (int i=0; i<length; i++)
            if (!ObjectUtils.equals(a[i], b[i]))
                return false;

        return true;
    }

}
