package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parser.TemplateBindingConsumer;

public class Template implements Locatable, TemplateBindingConsumer {
	public final InputPosition kw;
	private final InputPosition loc;
	public final TemplateName name;
	@Deprecated
	public final List<LocatedToken> args;
	@Deprecated
	public final TemplateLine content;

	public Template(InputPosition kw, InputPosition loc, TemplateName name, List<LocatedToken> args, TemplateLine content) {
		this.kw = kw;
		this.loc = loc;
		this.name = name;
		this.args = args;
		this.content = content;
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
		return "Template[" + name.uniqueName() + "]";
	}
}
