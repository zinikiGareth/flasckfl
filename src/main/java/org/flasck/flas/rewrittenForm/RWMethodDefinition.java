package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class RWMethodDefinition implements Serializable, Locatable {
	public final RWFunctionIntro intro;
	public final List<RWMethodCaseDefn> cases;
	
	public RWMethodDefinition(RWFunctionIntro intro, List<RWMethodCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
	
	@Override
	public InputPosition location() {
		return intro.location;
	}
	
	@Override
	public String toString() {
		return intro.toString();
	}
}
