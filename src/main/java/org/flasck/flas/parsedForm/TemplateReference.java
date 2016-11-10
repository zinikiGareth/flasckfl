package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateReference implements TemplateLine, Locatable {
	public final InputPosition location;
	public final String name;
	public final List<Object> args;

	public TemplateReference(InputPosition location, String name, List<Object> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}

	@Override
	public InputPosition location() {
		return location;
	}
}
