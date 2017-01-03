package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;
import org.zinutils.bytecode.NewMethodDefiner;

public interface TemplateGenerator {

	// TODO: this is clearly the wrong thing to return ... but what is the right thing?  this?
	NewMethodDefiner generateRender(String clz, AreaName areaName);

	AreaGenerator area(String clz, String base, String customTag);

}
