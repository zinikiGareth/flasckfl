package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public class MethodDefinition implements Serializable {
	public final FunctionIntro intro;
	public final List<MethodCaseDefn> cases;
	
	public MethodDefinition(FunctionIntro intro, List<MethodCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
	
	@Override
	public String toString() {
		return intro.toString();
	}
}
