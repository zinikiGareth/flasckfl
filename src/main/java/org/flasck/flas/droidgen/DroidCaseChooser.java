package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.template.CaseChooser;

public class DroidCaseChooser implements CaseChooser {

	public DroidCaseChooser(FunctionName sn) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public CaseChooser handleCase(FunctionName tfn) {
		return new DroidCaseChooser(tfn);
	}

	@Override
	public void code(AreaName cn) {
		// TODO Auto-generated method stub

	}

}
