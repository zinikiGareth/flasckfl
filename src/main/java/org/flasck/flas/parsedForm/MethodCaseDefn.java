package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class MethodCaseDefn {
	public final FunctionIntro intro;
	public final List<MethodMessage> messages = new ArrayList<MethodMessage>();

	public MethodCaseDefn(FunctionIntro fi) {
		intro = fi;
	}

	public void addMessage(MethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public String toString() {
		return "MCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
