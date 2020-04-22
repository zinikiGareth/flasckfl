package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parser.TemplateBindingConsumer;

public class Template implements Locatable, TemplateBindingConsumer {
	public final InputPosition kw;
	private final InputPosition loc;
	public final TemplateReference refersTo;

	public Template(InputPosition kw, InputPosition loc, TemplateReference refersTo) {
		this.kw = kw;
		this.loc = loc;
		this.refersTo = refersTo;
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public void addBinding(TemplateBinding binding) {
	}

	@Override
	public String toString() {
		return "Template[" + refersTo.name.uniqueName() + "]";
	}
}
