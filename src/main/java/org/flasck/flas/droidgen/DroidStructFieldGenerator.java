package org.flasck.flas.droidgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.rewrittenForm.FieldVisitor;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.TypeWithName;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.UtilException;

public class DroidStructFieldGenerator implements FieldVisitor {
	private final ByteCodeSink bcc;
	private final Access access;
	final Map<String, IFieldInfo> fields = new TreeMap<>();

	public DroidStructFieldGenerator(ByteCodeSink bcc, Access access) {
		this.bcc = bcc;
		this.access = access;
	}

	@Override
	public void visit(RWStructField sf) {
		JavaType jt;
		if (sf.type instanceof PrimitiveType) {
			PrimitiveType pt = (PrimitiveType)sf.type;
			if (pt.name().equals("Number"))
				jt = JavaType.int_; // what about floats?
			else if (pt.name().equals("String"))
				jt = JavaType.string;
			else if (pt.name().equals("Boolean"))
				jt = JavaType.boolean_;
			else
				throw new UtilException("Not handled " + sf.type);
		} else if (sf.type instanceof FunctionType) {
			jt = JavaType.object_;
		} else if (sf.type instanceof TypeWithName) {
			jt = javaType(((TypeWithName)sf.type).name());
		} else
			throw new UtilException("Not handled " + sf.type + " " + sf.type.getClass());
		IFieldInfo fi = bcc.defineField(false, access, jt, sf.name);
		fields.put(sf.name, fi);
	}
	
	@Deprecated
	private JavaType javaType(String name) {
		if (name.indexOf(".") == -1)
			name = "org.flasck.android.builtin." + name;
		return new JavaType(name);
	}
}
