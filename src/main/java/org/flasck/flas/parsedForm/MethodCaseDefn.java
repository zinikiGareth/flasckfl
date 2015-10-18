package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class MethodCaseDefn implements MessagesHandler, ContainsScope, Serializable {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	public final Scope scope;

	public MethodCaseDefn(FunctionIntro fi) {
		intro = fi;
		scope = null;
	}
	
	public MethodCaseDefn(ScopeEntry entry, MethodCaseDefn mcd) {
		this.scope = new Scope(entry);
		this.intro = mcd.intro;
	}

	public void addMessage(MethodMessage mm) {
		if (scope == null)
			throw new UtilException("Can't add messages to the one without the scope");
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
