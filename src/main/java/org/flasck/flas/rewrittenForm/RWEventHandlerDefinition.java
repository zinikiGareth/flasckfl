package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class RWEventHandlerDefinition implements Locatable, Serializable {
	public final RWFunctionIntro intro;
	public final List<RWEventCaseDefn> cases;
	
	public RWEventHandlerDefinition(RWFunctionIntro intro, List<RWEventCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}
}
