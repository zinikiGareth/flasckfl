package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWTemplateDiv extends RWTemplateFormatEvents {
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<RWTemplateLine> nested = new ArrayList<RWTemplateLine>();
	public List<String> droppables;

	public RWTemplateDiv(InputPosition kw, String customTag, String customTagVar, List<Object> attrs, AreaName areaName, List<Object> formats, FunctionName dynamicFn) {
		super(kw, areaName, formats, dynamicFn);
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
	}
}
