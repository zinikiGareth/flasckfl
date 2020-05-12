package org.flasck.flas.parsedForm;

import java.io.PrintWriter;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.Type;

public class TemplateNestedField implements RepositoryEntry {
	private final InputPosition loc;
	private final String reminder;
	private final StructField field;

	// This "reminder" field is here as much as anything to remind me that I am fairly confident I will ultimately want a chain of these things and this should give us a var name of some kind
	// It is almost undoubtedly not a string
	public TemplateNestedField(InputPosition loc, String reminder, StructField field) {
		this.loc = loc;
		this.reminder = reminder;
		this.field = field;
	}

	@Override
	public VarName name() {
		return field.name(); // or possibly not
	}
	
	public String reminder() {
		return reminder;
	}
	
	public Type type() {
		return field.type();
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
