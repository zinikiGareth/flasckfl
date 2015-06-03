package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.tokenizers.TemplateToken;

/* How to test this?
 * One option would be to compile the templates to javascript, then build one or more models,
 * then run the overall framework, calling into "renderFirstTime" (and/or "renderUpdate")
 * to create the relevant HTML using iojs, and then to compare the resultant "innerHTML" with
 * a "golden file".
 * 
 * Messy and slow, I know, but at least fairly realistic
 */
public class TemplateLine {
	public final List<Object> contents;
	public final List<TemplateToken> formats;
	public final String customTag;
	public final String customTagVar;

	public TemplateLine(List<Object> contents, String customTag, String customTagVar, List<TemplateToken> formats) {
		this.contents = contents;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.formats = formats;
	}
}
