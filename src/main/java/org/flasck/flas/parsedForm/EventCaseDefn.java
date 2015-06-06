package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class EventCaseDefn implements MessagesHandler {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();

	public EventCaseDefn(FunctionIntro fi) {
		intro = fi;
	}

	public void addMessage(MethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public String toString() {
		return "ECD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
