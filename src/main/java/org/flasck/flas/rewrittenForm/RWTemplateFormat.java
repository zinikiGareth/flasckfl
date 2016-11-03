package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.template.TemplateLine;

public abstract class RWTemplateFormat implements TemplateLine, Locatable {
	public final InputPosition kw;
	public final List<Object> formats;
	public final String dynamicFunction;
	
	public RWTemplateFormat(InputPosition kw, List<Object> formats, String dynamicFunction) {
		this.kw = kw;
		this.formats = formats;
		this.dynamicFunction = dynamicFunction;
	}

	@Override
	public InputPosition location() {
		return kw;
	}
}
