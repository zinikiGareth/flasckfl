package org.flasck.flas.rewrittenForm;

import java.util.List;

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

	public RWContentExpr(Object expr, List<Object> formats) {
		this(expr, false, false, formats);
	}
	
	public RWContentExpr(Object expr, boolean edit, boolean rawHTML, List<Object> formats) {
		super(formats);
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
