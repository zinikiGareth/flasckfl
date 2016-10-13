package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.template.TemplateFormat;

@SuppressWarnings("serial")
public abstract class RWTemplateFormatEvents extends TemplateFormat implements Serializable {
	public final List<RWEventHandler> handlers = new ArrayList<RWEventHandler>();
	
	public RWTemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
