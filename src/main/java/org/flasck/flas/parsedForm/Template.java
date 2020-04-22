package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parser.TemplateBindingConsumer;

public class Template implements Locatable, TemplateBindingConsumer {
	public final InputPosition kw;
	private final InputPosition loc;
	public final TemplateReference refersTo;
	private final List<TemplateBinding> bindings = new ArrayList<TemplateBinding>();

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
		bindings.add(binding);
	}
	
	public Iterable<TemplateBinding> bindings() {
		return bindings;
	}

	@Override
	public String toString() {
		return "Template[" + refersTo.name.uniqueName() + "]";
	}
}
