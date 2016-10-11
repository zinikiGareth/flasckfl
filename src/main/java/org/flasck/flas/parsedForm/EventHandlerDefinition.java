package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class EventHandlerDefinition implements Locatable, Serializable {
	public final FunctionIntro intro;
	public final List<EventCaseDefn> cases;
	
	public EventHandlerDefinition(FunctionIntro intro, List<EventCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}
}
