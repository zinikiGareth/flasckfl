package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;

public class Template {
	public final String prefix;
	public final Object content;
	public final Scope scope;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public Template(String prefix, Object content, Scope scope) {
		this.prefix = prefix;
		this.content = content;
		this.scope = scope;
	}
}
