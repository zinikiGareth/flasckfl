package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;

public class Template {

	public final String prefix;
	public final TemplateLine topLine;
	public final Scope scope;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public Template(String prefix, TemplateLine topLine, Scope scope) {
		this.prefix = prefix;
		this.topLine = topLine;
		this.scope = scope;
	}
}
