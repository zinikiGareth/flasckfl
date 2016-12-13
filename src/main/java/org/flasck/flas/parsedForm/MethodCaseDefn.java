package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.ScopeName;
import org.zinutils.exceptions.UtilException;

public class MethodCaseDefn implements Locatable, MessagesHandler, ContainsScope {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();
	public Scope scope;
	private ScopeName caseName;

	public MethodCaseDefn(FunctionIntro fi) {
		intro = fi;
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
	
	public FunctionName methodName() {
		return intro.name();
	}

	@Deprecated
	public String methodNameAsString() {
		return intro.name;
	}

	public void provideCaseName(int caseNum) {
		if (caseNum == -1)
			this.caseName = new ScopeName(this.intro.name().inContext, this.intro.name().name);
		else
			this.caseName = new ScopeName(this.intro.name().inContext, this.intro.name().name+"_"+caseNum);
		this.scope = new Scope(this.caseName);
	}

	@Deprecated
	public String caseNameAsString() {
		if (caseName == null)
			throw new UtilException("caseName has not yet been assigned");
		return caseName.jsName();
	}
	
	public ScopeName caseName() {
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
