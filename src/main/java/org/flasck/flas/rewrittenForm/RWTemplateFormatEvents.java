package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateFormat;

public abstract class RWTemplateFormatEvents extends TemplateFormat {
	public final List<RWEventHandler> handlers = new ArrayList<RWEventHandler>();
	
	public RWTemplateFormatEvents(InputPosition pos, List<Object> formats) {
		super(pos, formats);
	}
}
