package org.flasck.flas.parsedForm.template;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.EventHandler;

public abstract class TemplateFormatEvents extends TemplateFormat {
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();
	
	public TemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
