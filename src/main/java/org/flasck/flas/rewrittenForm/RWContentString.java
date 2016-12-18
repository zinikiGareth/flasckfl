package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;

public class RWContentString extends RWTemplateFormatEvents {
	public final String text;

	public RWContentString(InputPosition kw, String text, AreaName areaName, List<Object> formats, FunctionName dynamicFn) {
		super(kw, areaName, formats, dynamicFn);
		this.text = text;
	}
}
