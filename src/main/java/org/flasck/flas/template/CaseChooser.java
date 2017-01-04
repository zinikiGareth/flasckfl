package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;

public interface CaseChooser {

	CaseChooser handleCase(String tfn);

	void code(AreaName cn);

}
