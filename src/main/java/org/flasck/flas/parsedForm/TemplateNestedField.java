package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class TemplateNestedField implements RepositoryEntry {
	private final InputPosition loc;
	private final VarName name;
	private final Type type;
	private final StructField field;

	public TemplateNestedField(InputPosition loc, VarName name, Type type, StructField field) {
		this.loc = loc;
		this.name = name;
		this.type = type;
		this.field = field;
	}

	@Override
	public VarName name() {
		return this.name;
	}
	
	public Type type() {
		return type;
	}
	
	public StructField getField() {
		return field;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("TNF[" + name() + "]");
	}

}
