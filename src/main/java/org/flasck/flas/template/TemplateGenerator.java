package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;

public interface TemplateGenerator {

	void generateRender(String clz, AreaName areaName);

	AreaGenerator area(AreaName areaName, String base, String customTag, String nsTag, Object wantCard, Object wantYoyo);

}
