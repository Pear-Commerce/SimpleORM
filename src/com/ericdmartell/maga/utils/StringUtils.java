package com.ericdmartell.maga.utils;

public class StringUtils {
	public static String defaultString(String string, String def) {
		if (string != null) {
			return string;
		}
		return def;
		
	}
}
