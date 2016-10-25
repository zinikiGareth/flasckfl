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

	public RWContentExpr(InputPosition kw, Object expr, List<Object> formats) {
		this(kw, expr, false, false, formats);
	}
	
	public RWContentExpr(InputPosition kw, Object expr, boolean edit, boolean rawHTML, List<Object> formats) {
		super(kw, formats);
		this.expr = expr;
		this.editable = edit;
		this.rawHTML = rawHTML;
	}

	public void makeEditable() {
		this.editable = true;
	}
	
	public boolean editable() {
		return this.editable;
	}
}
