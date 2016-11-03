package org.flasck.flas.commonBase.template;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public abstract class TemplateFormat implements TemplateLine, Locatable {
	public final InputPosition kw;
	public final List<Object> formats;
	
	public TemplateFormat(InputPosition kw, List<Object> formats) {
		this.kw = kw;
		this.formats = formats;
	}

	@Override
	public InputPosition location() {
		return kw;
	}
}
