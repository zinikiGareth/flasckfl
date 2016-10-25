package org.flasck.flas.commonBase.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;

public class Template implements Locatable {
	public final InputPosition kw;
	private final InputPosition loc;
	public final String prefix;
	public final TemplateLine content;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public Template(InputPosition kw, InputPosition loc, String prefix, TemplateLine content) {
		this.kw = kw;
		this.loc = loc;
		this.prefix = prefix;
		this.content = content;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
}
