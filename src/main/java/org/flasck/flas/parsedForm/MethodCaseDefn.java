package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

public class MethodCaseDefn implements MessagesHandler, ContainsScope {
	public final FunctionIntro intro;
	private final int cs;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	public final Scope scope;

	public MethodCaseDefn(FunctionIntro fi, int cs) {
		intro = fi;
		this.cs = cs;
		scope = null;
	}
	
	public MethodCaseDefn(ScopeEntry entry, MethodCaseDefn mcd, int cs) {
		this.scope = new Scope(entry, this);
		this.cs = cs;
		this.intro = mcd.intro;
	}

	public void addMessage(MethodMessage mm) {
		if (scope == null)
			throw new UtilException("Can't add messages to the one without the scope");
		messages.add(mm);
	}
	
	public String methodName() {
		return intro.name;
	}

	public String caseName() {
		if (cs == -1)
			return intro.name;
		else
			return intro.name +"_" + cs;
	}

	public int nargs() {
		return intro.args.size();
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
