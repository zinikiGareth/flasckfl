package org.flasck.flas.parsedForm.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.EventHandler;

@SuppressWarnings("serial")
public abstract class TemplateFormatEvents extends TemplateFormat implements Serializable {
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();
	
	public TemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
