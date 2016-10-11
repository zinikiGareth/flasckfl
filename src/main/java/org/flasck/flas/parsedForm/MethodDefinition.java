package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class MethodDefinition implements Serializable, Locatable {
	public final FunctionIntro intro;
	public final List<MethodCaseDefn> cases;
	
	public MethodDefinition(FunctionIntro intro, List<MethodCaseDefn> list) {
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
