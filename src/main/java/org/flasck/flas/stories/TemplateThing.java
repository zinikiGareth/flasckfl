package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.template.TemplateLine;

public class TemplateThing {
	public final String name;
	public final List<LocatedToken> args;
	public final TemplateLine content;

	public TemplateThing(String name, List<LocatedToken> args, TemplateLine content) {
		this.name = name;
		this.args = args;
		this.content = content;
	}

	@Override
	public String toString() {
		return "TemplateThing[" + name + "]";
	}
}
