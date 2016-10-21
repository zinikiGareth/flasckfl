package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class RWEventHandlerDefinition implements Locatable {
	public final List<RWEventCaseDefn> cases = new ArrayList<>();
	private InputPosition loc;
	private String name;
	private int nargs;
	
	public RWEventHandlerDefinition(InputPosition loc, String name, int nargs) {
		this.loc = loc;
		this.name = name;
		this.nargs = nargs;
	}
	
	@Override
	public InputPosition location() {
		return loc;
	}
	
	public String name() {
		return name;
	}
	
	public int nargs() {
		return nargs;
	}
}
