package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateLine;

public class RWTemplateDiv extends RWTemplateFormatEvents {
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();
	public List<String> droppables;

	public RWTemplateDiv(InputPosition kw, String customTag, String customTagVar, List<Object> attrs, List<Object> formats, String dynamicFn) {
		super(kw, formats, dynamicFn);
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
	}
}
