package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class EventCaseDefn implements MessagesHandler, ContainsScope, Serializable {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	private final Scope scope;

	public EventCaseDefn(FunctionIntro fi) {
		intro = fi;
		this.scope = null;
	}

	public EventCaseDefn(ScopeEntry se, EventCaseDefn starter) {
		this.intro = starter.intro;
		this.scope = new Scope(se);
	}

	public void addMessage(MethodMessage mm) {
		if (scope == null)
			throw new UtilException("Cannot add messages to starter version");
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
