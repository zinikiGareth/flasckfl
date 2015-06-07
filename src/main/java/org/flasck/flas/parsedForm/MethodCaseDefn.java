package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class MethodCaseDefn implements MessagesHandler, ContainsScope {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	public final Scope scope;

	public MethodCaseDefn(FunctionIntro fi) {
		intro = fi;
		scope = new Scope((Scope)null); // TODO: null is not really acceptable
	}

	public void addMessage(MethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public Scope innerScope() {
		return scope;
	}
	
	@Override
	public String toString() {
		return "MCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
