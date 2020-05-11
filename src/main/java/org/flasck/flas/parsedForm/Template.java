package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parser.TemplateBindingConsumer;
import org.flasck.flas.repository.RepositoryEntry;

public class Template implements Locatable, RepositoryEntry, TemplateBindingConsumer {
	public final InputPosition kw;
	private final InputPosition loc;
	public final TemplateReference defines;
	private final List<TemplateBinding> bindings = new ArrayList<TemplateBinding>();

	public Template(InputPosition kw, InputPosition loc, TemplateReference defines) {
		this.kw = kw;
		this.loc = loc;
		this.defines = defines;
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
	public void dumpTo(PrintWriter pw) {
		pw.println(this);
	}

	@Override
	public String toString() {
		return "Template[" + defines.name.uniqueName() + "]";
	}

	public TemplateName name() {
		return defines.name;
	}
}
