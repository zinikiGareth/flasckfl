package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.TemplateLine;

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
