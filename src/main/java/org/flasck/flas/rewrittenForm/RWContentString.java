package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.List;

/* How to test this?
 * One option would be to compile the templates to javascript, then build one or more models,
 * then run the overall framework, calling into "renderFirstTime" (and/or "renderUpdate")
 * to create the relevant HTML using iojs, and then to compare the resultant "innerHTML" with
 * a "golden file".
 * 
 * Messy and slow, I know, but at least fairly realistic
 */
@SuppressWarnings("serial")
public class RWContentString extends RWTemplateFormatEvents implements Serializable {
	public final String text;

	public RWContentString(String text, List<Object> formats) {
		super(formats);
		this.text = text;
	}
}
