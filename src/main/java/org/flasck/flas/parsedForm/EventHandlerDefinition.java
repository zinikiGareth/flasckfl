package org.flasck.flas.parsedForm;

import java.util.List;

public class EventHandlerDefinition {
	public final FunctionIntro intro;
	public final List<EventCaseDefn> cases;
	
	public EventHandlerDefinition(FunctionIntro intro, List<EventCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
}
