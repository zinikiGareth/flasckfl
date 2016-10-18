package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.template.TemplateFormat;

public abstract class RWTemplateFormatEvents extends TemplateFormat {
	public final List<RWEventHandler> handlers = new ArrayList<RWEventHandler>();
	
	public RWTemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
