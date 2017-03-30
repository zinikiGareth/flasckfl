package org.flasck.flas.droidgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.rewrittenForm.FieldVisitor;
import org.flasck.flas.rewrittenForm.RWStructField;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;

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
		IFieldInfo fi = bcc.defineField(false, access, JavaType.object_, sf.name);
		fields.put(sf.name, fi);
	}
}
