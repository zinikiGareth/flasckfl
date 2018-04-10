package org.flasck.flas.jsgen;

import java.util.Arrays;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.TemplateGenerator;

public class JSTemplateGenerator implements TemplateGenerator {
	private final JSTarget target;

	public JSTemplateGenerator(JSTarget target) {
		this.target = target;
	}

	@Override
	public void generateRender(TemplateName tname, AreaName areaName) {
		FunctionName render = FunctionName.functionInCardContext(null, tname.containingCard(), "_render");
		JSForm ir = JSForm.flexFn(render.jsPName(), Arrays.asList("doc", "wrapper", "parent"));
		target.add(ir);
		if (areaName != null)
			ir.add(JSForm.flex("new " + areaName.jsName() + "(new CardArea(parent, wrapper, this))"));
	}

	@Override
	public AreaGenerator area(AreaName areaName, String base, String customTag, String nsTag, Object wantCard, Object wantYoyo, String webzip) {
		JSForm fn = JSForm.flex(areaName.jsName() +" = function(parent)").needBlock();
		target.add(fn);
		target.add(JSForm.flex(areaName.jsName() +".prototype = new " + base + "()"));
		target.add(JSForm.flex(areaName.jsName() +".prototype.constructor = " + areaName.jsName()));
		String moreArgs = "";
		if (wantYoyo != null)
			moreArgs = ", undefined"; // explicitly say the card is undefined until yoyoVar evaluates
		else if (wantCard != null)
			moreArgs = ", { explicit: " + wantCard + "}";
		else if (customTag != null) {
			moreArgs = ", '" + customTag + "'";
			if (nsTag != null)
				moreArgs = moreArgs + ", " + nsTag;
		} else if (webzip != null) {
			// TODO: need to do some kind of escaping for quotes and newlines at least
			moreArgs = ", null, null, '" + webzip + "'";
		}
		fn.add(JSForm.flex(base +".call(this, parent" + moreArgs + ")"));
		fn.add(JSForm.flex("if (!parent) return"));
		return new JSAreaGenerator(target, fn, areaName);
	}

}
