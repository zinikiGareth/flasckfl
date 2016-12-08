package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

/* How to test this?
 * One option would be to compile the templates to javascript, then build one or more models,
 * then run the overall framework, calling into "renderFirstTime" (and/or "renderUpdate")
 * to create the relevant HTML using iojs, and then to compare the resultant "innerHTML" with
 * a "golden file".
 * 
 * Messy and slow, I know, but at least fairly realistic
 */
public class RWContentExpr extends RWTemplateFormatEvents {
	public final Object expr;
	private boolean editable;
	public final boolean rawHTML;
	public final String fnName;
	public final String editFn;

	public RWContentExpr(InputPosition kw, Object expr, boolean edit, boolean rawHTML, AreaName areaName, List<Object> formats, String fnName, String dynamicFn, String editFn) {
		super(kw, areaName, formats, dynamicFn);
		this.expr = expr;
		this.editable = edit;
		this.rawHTML = rawHTML;
		this.fnName = fnName;
		this.editFn = editFn;
	}

	public void makeEditable() {
		this.editable = true;
	}
	
	public boolean editable() {
		return this.editable;
	}
}
