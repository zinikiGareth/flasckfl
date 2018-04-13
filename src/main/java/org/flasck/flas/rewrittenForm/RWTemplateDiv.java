package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.htmlzip.Block;

public class RWTemplateDiv extends RWTemplateFormatEvents {
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<RWTemplateLine> nested = new ArrayList<RWTemplateLine>();
	public List<String> droppables;
	public final Block webzip;
	public final String holeName;

	public RWTemplateDiv(InputPosition kw, Block webzip, String customTag, String customTagVar, List<Object> attrs, AreaName areaName, List<Object> formats, FunctionName dynamicFn, String wzblock) {
		super(kw, areaName, formats, dynamicFn);
		this.webzip = webzip;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
		this.holeName = wzblock;
	}
}
