package org.flasck.flas.parsedForm;

import java.util.List;

public class MethodDefinition {
	public final FunctionIntro intro;
	public final List<MethodCaseDefn> cases;
	
	public MethodDefinition(FunctionIntro intro, List<MethodCaseDefn> list) {
		this.intro = intro;
		this.cases = list;
	}
}
