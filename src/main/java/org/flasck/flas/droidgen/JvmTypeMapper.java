package org.flasck.flas.droidgen;

import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PolyVar;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.TypeWithName;
import org.flasck.jvm.post.TypeOf;
import org.zinutils.bytecode.JavaType;
import org.zinutils.exceptions.UtilException;

public class JvmTypeMapper {

	public static JavaType map(Type type) {
		if (type instanceof PrimitiveType) {
			PrimitiveType pt = (PrimitiveType)type;
			if (pt.name().equals("Number"))
				return JavaType.int_; // what about floats?
			else if (pt.name().equals("String"))
				return JavaType.string;
			else if (pt.name().equals("Boolean"))
				return JavaType.boolean_;
			else if (pt.name().equals("Type"))
				return new JavaType(TypeOf.class.getName());
			else
				throw new UtilException("Not handled " + type);
		} else if (type instanceof FunctionType) {
			return JavaType.object_;
		} else if (type instanceof PolyVar) {
			return JavaType.object_;
		} else if (type instanceof TypeWithName) {
			String name = ((TypeWithName)type).getName().javaName();
			if (name.equals("Any"))
				return JavaType.object_;
			else
				return new JavaType(name);
		} else
			throw new UtilException("Not handled " + type + " " + type.getClass());
	}

}
