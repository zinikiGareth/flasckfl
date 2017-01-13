package org.flasck.flas.droidgen;

import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.types.FunctionType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringUtil;

public class DroidUtils {

	public static String getJavaClassForDefn(NewMethodDefiner meth, ExternalRef name, Object defn) {
		int idx = name.uniqueName().lastIndexOf(".");
		String inside;
		String dot;
		String member;
		if (idx == -1) {
			inside = J.BUILTINPKG;
			dot = ".";
			member = name.uniqueName();
		} else {
			String first = name.uniqueName().substring(0, idx);
			if ("FLEval".equals(first)) {
				inside = J.FLEVAL;
				member = StringUtil.capitalize(name.uniqueName().substring(idx+1));
			} else {
				inside = name.uniqueName().substring(0, idx);
				member = name.uniqueName().substring(idx+1);
			}
			dot = "$";
		}
		String clz;
		if (defn instanceof RWFunctionDefinition || defn instanceof RWMethodDefinition || defn instanceof FunctionType) {
			if (inside.equals(J.FLEVAL))
				clz = inside + "$" + member;
			else
				clz = inside + ".PACKAGEFUNCTIONS$" + member;
		} else {
			clz = inside + dot + member;
		}
		meth.getBCC().addInnerClassReference(Access.PUBLICSTATIC, inside, member);
		return clz;
	}

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
