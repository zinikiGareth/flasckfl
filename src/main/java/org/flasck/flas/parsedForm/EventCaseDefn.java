package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.ScopeName;
import org.zinutils.exceptions.UtilException;

public class EventCaseDefn implements Locatable, MessagesHandler, ContainsScope {
	public final InputPosition kw;
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	private Scope scope;
	private ScopeName caseName;

	public EventCaseDefn(InputPosition kw, FunctionIntro fi) {
		this.kw = kw;
		intro = fi;
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}

	public String methodName() {
		return intro.name;
	}

	public void provideCaseName(int caseNum) {
		this.caseName = new ScopeName(this.intro.name().inContext, this.intro.name().name+"_"+caseNum);
		this.scope = new Scope(this.caseName);
	}

	public String caseNameAsString() {
		return caseName.jsName();
	}
	
	public ScopeName caseName() {
		return caseName;
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
