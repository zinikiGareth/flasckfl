package org.flasck.flas.parsedForm;

import java.util.ArrayList;
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
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<TemplateToken> formats;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();

	public TemplateLine(List<Object> contents, String customTag, String customTagVar, List<Object> attrs, List<TemplateToken> formats) {
		this.contents = contents;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
		this.formats = formats;
	}

	public boolean isDiv() {
		if (contents.isEmpty())
			return true;
		if (contents.size() > 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateToken && ((TemplateToken)o).type == TemplateToken.DIV;
	}

	public boolean isList() {
		if (contents.size() != 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateList;
	}
}
