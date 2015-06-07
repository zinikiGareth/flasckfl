package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class EventCaseDefn implements MessagesHandler, ContainsScope {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	private final Scope scope;

	public EventCaseDefn(FunctionIntro fi) {
		intro = fi;
		this.scope = new Scope((Scope)null); // TODO: null is not really acceptable
	}

	public void addMessage(MethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public String toString() {
		return "ECD[" + intro.name + "/" + intro.args.size() + "]";
	}

	public Scope innerScope() {
		return scope;
	}
}
