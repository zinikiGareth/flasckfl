package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RWMethodCaseDefn {
	public final RWFunctionIntro intro;
	public final List<RWMethodMessage> messages = new ArrayList<RWMethodMessage>();

	public RWMethodCaseDefn(RWFunctionIntro fi) {
		intro = fi;
	}
	
	public void addMessage(RWMethodMessage mm) {
		messages.add(mm);
	}
	
	public void gatherScopedVars(Set<VarNestedFromOuterFunctionScope> scopedVars) {
		for (RWMethodMessage m : messages)
			m.gatherScopedVars(scopedVars);
	}
	
	@Override
	public String toString() {
		return "MCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
