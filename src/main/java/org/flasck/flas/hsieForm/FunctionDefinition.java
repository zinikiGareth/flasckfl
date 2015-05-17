package org.flasck.flas.hsieForm;

import java.util.List;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.zinutils.exceptions.UtilException;

public class FunctionDefinition {
	public final List<FunctionCaseDefn> cases;
	private HSIEForm hsie;

	public FunctionDefinition(List<FunctionCaseDefn> defns) {
		this.cases = defns;
	}
	
	public FunctionDefinition setHSIE(HSIEForm form) {
		if (hsie != null)
			throw new UtilException("Multiple specifications of HSIE");
		this.hsie = form;
		return this;
	}
	
	public HSIEForm get() {
		return hsie;
	}
	
	// FunctionDefinition also has nested Scope
}
