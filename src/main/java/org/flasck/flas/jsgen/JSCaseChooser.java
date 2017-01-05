package org.flasck.flas.jsgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.template.CaseChooser;

public class JSCaseChooser implements CaseChooser {
	private final JSForm sw;

	public JSCaseChooser(JSForm sw) {
		this.sw = sw;
	}

	@Override
	public CaseChooser handleCase(FunctionName tfn) {
		JSForm doit = JSForm.flex("if (FLEval.full(this." + tfn.name + "()))").needBlock();
		sw.add(doit);
		return new JSCaseChooser(doit);
	}

	@Override
	public void code(AreaName cn) {
		sw.add(JSForm.flex("this._setTo(" + cn.jsName() +")"));
		sw.add(JSForm.flex("return"));
	}

	
}
