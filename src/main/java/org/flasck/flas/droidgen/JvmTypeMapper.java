package org.flasck.flas.droidgen;

import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeWithName;
import org.flasck.jvm.builtin.FLList;
import org.zinutils.bytecode.JavaType;
import org.zinutils.exceptions.UtilException;

public class JvmTypeMapper {

	public static JavaType map(Type type) {
		if (type instanceof PrimitiveType) {
			PrimitiveType pt = (PrimitiveType)type;
			if (pt.nameAsString().equals("Number"))
				return new JavaType("org.flasck.jvm.builtin.FLNumber");
			else if (pt.nameAsString().equals("String"))
				return JavaType.string;
			else if (pt.nameAsString().equals("Boolean"))
				return JavaType.boolean_;
			else if (pt.nameAsString().equals("Type"))
				return new JavaType(Object.class.getName());
			else
				throw new UtilException("Not handled " + type);
		} else if (type instanceof FunctionType) {
			return JavaType.object_;
		} else if (type instanceof PolyVar) {
			return JavaType.object_;
		} else if (type instanceof TypeWithName) {
			String name = ((TypeWithName)type).getName().javaClassName();
			if (name.equals("org.flasck.jvm.builtin.Any"))
				return JavaType.object_;
			else if (name.equals("org.flasck.jvm.builtin.List"))
				return new JavaType(FLList.class.getName());
			else
				return new JavaType(name);
		} else
			throw new UtilException("Not handled " + type + " " + type.getClass());
	}

}
