package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public abstract class TemplateFormatEvents extends TemplateFormat implements Serializable {
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();
	
	public TemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
