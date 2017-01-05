package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.TemplateName;

public class RWTemplate implements Locatable {
	public final InputPosition kw;
	private final InputPosition loc;
	public final RWTemplateLine content;
	public final List<LocatedToken> args = new ArrayList<LocatedToken>();
	public final TemplateName tname;
	
	public RWTemplate(InputPosition kw, InputPosition loc, TemplateName prefix, RWTemplateLine content) {
		this.kw = kw;
		this.loc = loc;
		this.tname = prefix;
		this.content = content;
	}

	@Override
	public InputPosition location() {
		return loc;
	}
	
	public AreaName areaName() {
		return content.areaName();
	}
	
	@Override
	public String toString() {
		return "RWTemplate[" + tname.uniqueName() + "]";
	}
}
