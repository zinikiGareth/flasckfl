package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.parsedForm.TemplateLine;

public class TemplateThing {
	public final String name;
	public final List<LocatedToken> args;
	public final TemplateLine lines;

	public TemplateThing(String name, List<LocatedToken> args, TemplateLine lines) {
		this.name = name;
		this.args = args;
		this.lines = lines;
	}


}
