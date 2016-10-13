package org.flasck.flas.commonBase.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;

@SuppressWarnings("serial")
public class Template implements Serializable{
	public final String prefix;
	public final TemplateLine content;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public Template(String prefix, TemplateLine content) {
		this.prefix = prefix;
		this.content = content;
	}
}
