package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;

public interface CaseChooser {

	CaseChooser handleCase(FunctionName tfn);

	void code(AreaName cn);

}
