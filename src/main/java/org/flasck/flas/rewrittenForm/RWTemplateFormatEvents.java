package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;

public abstract class RWTemplateFormatEvents extends RWTemplateFormat {
	public final List<RWEventHandler> handlers = new ArrayList<RWEventHandler>();
	
	public RWTemplateFormatEvents(InputPosition pos, AreaName areaName, List<Object> formats, String dynamicFn) {
		super(pos, areaName, formats, dynamicFn);
	}
}
