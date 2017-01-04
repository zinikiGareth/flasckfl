package org.flasck.flas.droidgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.rewrittenForm.FieldVisitor;
import org.flasck.flas.rewrittenForm.RWContractDecl;
import org.flasck.flas.rewrittenForm.RWContractImplements;
import org.flasck.flas.rewrittenForm.RWObjectDefn;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.flasck.flas.types.PrimitiveType;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.Type.WhatAmI;
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
			if (((Type)sf.type).name().equals("Number"))
				jt = JavaType.int_; // what about floats?
			else if (((Type)sf.type).name().equals("String"))
				jt = JavaType.string;
			else if (((Type)sf.type).name().equals("Boolean"))
				jt = JavaType.boolean_;
			else
				throw new UtilException("Not handled " + sf.type);
		} else if (sf.type instanceof RWContractImplements || sf.type instanceof RWContractDecl) {
			jt = javaType(sf.type.name());
		} else if (sf.type instanceof RWObjectDefn) {
			jt = javaType(sf.type.name());
		} else if (sf.type instanceof Type) {
			if (sf.type.iam == WhatAmI.FUNCTION)
				jt = JavaType.object_;
			else
				jt = javaType(sf.type.name());
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
