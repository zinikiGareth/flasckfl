package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWEventHandlerDefinition implements Locatable {
	public final List<RWEventCaseDefn> cases = new ArrayList<>();
//	public final String cardName;
//	private InputPosition loc;
//	private String name;
	private int nargs;
	private FunctionName fnName;
	
	public RWEventHandlerDefinition(FunctionName name, int nargs) {
		this.fnName = name;
		this.nargs = nargs;
	}

	@Override
	public InputPosition location() {
		return fnName.location;
	}
	
	public FunctionName name() {
		return fnName;
	}
	
	public int nargs() {
		return nargs;
	}
}
