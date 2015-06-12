package org.flasck.flas.parsedForm;

public class Template {

	public final String prefix;
	public final String name;
	public final TemplateLine topLine;
	public final Scope scope;
	
	public Template(String prefix, String name, TemplateLine topLine, Scope scope) {
		this.prefix = prefix;
		this.name = name;
		this.topLine = topLine;
		this.scope = scope;
	}
}
