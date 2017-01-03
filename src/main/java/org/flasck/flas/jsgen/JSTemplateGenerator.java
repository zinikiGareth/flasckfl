package org.flasck.flas.jsgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.TemplateGenerator;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.collections.CollectionUtils;

public class JSTemplateGenerator implements TemplateGenerator {
	private final JSTarget target;

	public JSTemplateGenerator(JSTarget target) {
		this.target = target;
	}

	@Override
	public NewMethodDefiner generateRender(String clz, AreaName areaName) {
		JSForm ir = JSForm.flexFn(clz + "_render", CollectionUtils.listOf("doc", "wrapper", "parent"));
		target.add(ir);
		if (areaName != null)
			ir.add(JSForm.flex("new " + areaName.jsName() + "(new CardArea(parent, wrapper, this))"));
		return null;
	}

	@Override
	public AreaGenerator area(String clz, String base, String customTag) {
		// TODO Auto-generated method stub
		return null;
	}

}
