package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.zinutils.exceptions.UtilException;

public class MethodCaseDefn implements Locatable, MessagesHandler, ContainsScope {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	public final Scope scope;
	private String caseName;

	public MethodCaseDefn(FunctionIntro fi) {
		intro = fi;
		scope = new Scope(fi.name());
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}

	public void addMessage(MethodMessage mm) {
		if (scope == null)
			throw new UtilException("Can't add messages to the one without the scope");
		messages.add(mm);
	}
	
	public String methodName() {
		return intro.name;
	}

	public void provideCaseName(String caseName) {
		this.caseName = caseName;
	}

	public String caseName() {
		if (caseName == null)
			throw new UtilException("caseName has not yet been assigned");
		return caseName;
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
