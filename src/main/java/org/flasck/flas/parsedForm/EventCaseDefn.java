package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

public class EventCaseDefn implements MessagesHandler, ContainsScope {
	public final InputPosition kw;
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	private final Scope scope;

	public EventCaseDefn(InputPosition kw, FunctionIntro fi) {
		this.kw = kw;
		intro = fi;
		this.scope = null;
	}

	public EventCaseDefn(ScopeEntry se, EventCaseDefn starter) {
		this.kw = starter.kw;
		this.intro = starter.intro;
		this.scope = new Scope(se, this);
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
