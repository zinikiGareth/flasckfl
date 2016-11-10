package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public abstract class TemplateFormatEvents extends TemplateFormat {
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();
	
	public TemplateFormatEvents(InputPosition location, List<Object> formats) {
		super(location, formats);
	}
}
