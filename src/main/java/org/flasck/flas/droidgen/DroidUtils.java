package org.flasck.flas.droidgen;

import org.zinutils.exceptions.UtilException;

public class DroidUtils {

	public static String javaBaseName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx);
	}

	static String javaNestedName(String clz) {
		if (clz.indexOf("$") != -1)
			throw new UtilException("Nested of nested?");
		int idx = clz.lastIndexOf(".");
		return clz.substring(0, idx) + "$" + clz.substring(idx+1);
	}

	public static String javaNestedSimpleName(String clz) {
		int idx = clz.lastIndexOf(".");
		return clz.substring(idx+1);
	}

}