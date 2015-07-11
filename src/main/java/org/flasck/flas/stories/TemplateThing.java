package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;

public class TemplateThing {
	public final String name;
	public final List<LocatedToken> args;
	public final Object content;

	public TemplateThing(String name, List<LocatedToken> args, Object content) {
		this.name = name;
		this.args = args;
		this.content = content;
	}


}
