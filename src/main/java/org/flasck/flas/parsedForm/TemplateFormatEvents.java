package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public abstract class TemplateFormatEvents extends TemplateFormat {
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();
	
	public TemplateFormatEvents(List<Object> formats) {
		super(formats);
	}
}
