package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public class RWTemplate implements Serializable{
	public final String prefix;
	public final TemplateLine content;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public RWTemplate(String prefix, TemplateLine content) {
		this.prefix = prefix;
		this.content = content;
	}
}
