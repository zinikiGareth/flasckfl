package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;

public class RWTemplate implements Locatable {
	public final InputPosition kw;
	private final InputPosition loc;
	public final String prefix;
	public final RWTemplateLine content;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	
	public RWTemplate(InputPosition kw, InputPosition loc, String prefix, RWTemplateLine content) {
		this.kw = kw;
		this.loc = loc;
		this.prefix = prefix;
		this.content = content;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
	
	public String areaName() {
		return content.areaName();
	}
	
	@Override
	public String toString() {
		return "RWTemplate[" + prefix + "]";
	}
}
