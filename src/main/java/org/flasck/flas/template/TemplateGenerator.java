package org.flasck.flas.template;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.htmlzip.Block;

public interface TemplateGenerator {

	void generateRender(TemplateName tname, AreaName areaName);

	AreaGenerator area(AreaName areaName, String base, String customTag, String nsTag, Object wantCard, Object wantYoyo, Block webzip);

}
