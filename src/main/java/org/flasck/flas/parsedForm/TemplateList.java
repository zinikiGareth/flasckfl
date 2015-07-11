package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateList extends TemplateFormat {
	public final InputPosition listLoc;
	public final Object listVar;
	public final Object iterVar;
	public Object template;

	public TemplateList(InputPosition listLoc, Object listVar, Object iterVar, List<Object> formats) {
		super(formats);
		this.listLoc = listLoc;
		this.listVar = listVar;
		this.iterVar = iterVar;
	}

}
