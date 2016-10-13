package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class RWMethodDefinition implements Serializable, Locatable {
	public final RWFunctionIntro intro;
	public final List<RWMethodCaseDefn> cases = new ArrayList<>();
	
	public RWMethodDefinition(RWFunctionIntro intro) {
		this.intro = intro;
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
