package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class RWEventHandlerDefinition implements Locatable {
	public final RWFunctionIntro intro;
	public final List<RWEventCaseDefn> cases = new ArrayList<>();
	
	public RWEventHandlerDefinition(RWFunctionIntro intro) {
		this.intro = intro;
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}
}
