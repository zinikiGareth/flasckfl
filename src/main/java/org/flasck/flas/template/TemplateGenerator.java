package org.flasck.flas.template;

import org.zinutils.bytecode.NewMethodDefiner;

public interface TemplateGenerator {

	NewMethodDefiner generateRender(String clz, String topBlock);

	AreaGenerator area(String clz, String base, String customTag);

}
