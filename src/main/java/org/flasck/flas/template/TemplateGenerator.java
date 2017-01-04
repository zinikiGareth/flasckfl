package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;

public interface TemplateGenerator {

	void generateRender(String clz, AreaName areaName);

	AreaGenerator area(String clz, String base, String customTag);

}
