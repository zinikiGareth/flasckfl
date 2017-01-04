package org.flasck.flas.droidgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.template.CaseChooser;

public class DroidCaseChooser implements CaseChooser {

	public DroidCaseChooser(String sn) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public CaseChooser handleCase(String tfn) {
		return new DroidCaseChooser(tfn);
	}

	@Override
	public void code(AreaName cn) {
		// TODO Auto-generated method stub

	}

}
