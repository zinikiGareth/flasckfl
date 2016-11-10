package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;

public class Template implements Locatable {
	public final InputPosition kw;
	private final InputPosition loc;
	public final String name;
	public final List<LocatedToken> args;
	public final TemplateLine content;

	public Template(InputPosition kw, InputPosition loc, String name, List<LocatedToken> args, TemplateLine content) {
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
	public String toString() {
		return "Template[" + name + "]";
	}
}
