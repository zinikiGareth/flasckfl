package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateList {
	public final InputPosition listLoc;
	public final Object listVar;
	public final Object iterVar;

	public TemplateList(InputPosition listLoc, Object listVar, Object iterVar) {
		this.listLoc = listLoc;
		this.listVar = listVar;
		this.iterVar = iterVar;
	}

}
