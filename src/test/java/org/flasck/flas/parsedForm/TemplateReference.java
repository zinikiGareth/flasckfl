package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateReference implements TemplateLine {
	public final InputPosition location;
	public final String name;
	public final List<Object> args;

	public TemplateReference(InputPosition location, String name, List<Object> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}
}
