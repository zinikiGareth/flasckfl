package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class RWEventCaseDefn {
	public final InputPosition kw;
	public final RWFunctionIntro intro;
	public final List<RWMethodMessage> messages = new ArrayList<RWMethodMessage>();

	public RWEventCaseDefn(InputPosition kw, RWFunctionIntro fi) {
		this.kw = kw;
		intro = fi;
	}

	public void addMessage(RWMethodMessage mm) {
		messages.add(mm);
	}
	
	@Override
	public String toString() {
		return "ECD[" + intro.name + "/" + intro.args.size() + "]";
	}
}
