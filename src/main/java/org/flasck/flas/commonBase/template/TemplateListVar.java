package org.flasck.flas.commonBase.template;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class TemplateListVar implements Locatable {
	public final InputPosition location;
	public final String dataFuncName; // the name of the function that generates the data
	public final String simpleName;
	public final String realName;

	public TemplateListVar(InputPosition location, String dataFuncName, String simpleName, String realName) {
		this.location = location;
		this.dataFuncName = dataFuncName;
		this.simpleName = simpleName;
		this.realName = realName;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public String toString() {
		return "TLV[" + realName + "<-" + dataFuncName + "]";
	}
}
