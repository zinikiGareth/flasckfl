package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public abstract class RWTemplateFormat implements RWTemplateLine, Locatable {
	public final InputPosition kw;
	private final AreaName areaName;
	public final List<Object> formats;
	public final String dynamicFunction;
	
	public RWTemplateFormat(InputPosition kw, AreaName areaName, List<Object> formats, String dynamicFunction) {
		this.kw = kw;
		this.areaName = areaName;
		this.formats = formats;
		this.dynamicFunction = dynamicFunction;
	}

	@Override
	public InputPosition location() {
		return kw;
	}

	@Override
	public AreaName areaName() {
		return areaName;
	}
}
