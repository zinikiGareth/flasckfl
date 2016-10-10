package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class RWMethodCaseDefn implements MessagesHandler, Serializable {
	public final RWFunctionIntro intro;
	public final List<RWMethodMessage> messages = new ArrayList<RWMethodMessage>();

	public RWMethodCaseDefn(RWFunctionIntro fi) {
		intro = fi;
	}
	
	public void addMessage(RWMethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public String toString() {
		return "MCD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
