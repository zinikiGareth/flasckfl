package org.flasck.flas.jsgen;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.template.EventHandlerGenerator;

public class JSEventHandlerGenerator implements EventHandlerGenerator {
	private final JSForm ahf;

	public JSEventHandlerGenerator(JSForm ahf) {
		this.ahf = ahf;
	}

	@Override
	public void handle(boolean giveDistinguishedName, String action, FunctionName callFn) {
		String distinguish = giveDistinguishedName?"_":"";
		JSForm cev = JSForm.flex("this._mydiv['on" + distinguish + action + "'] = function(event)").needBlock();
		cev.add(JSForm.flex("this._area._wrapper.dispatchEvent(this._area." + callFn.name + "(), event)"));
		ahf.add(cev);
	}

	@Override
	public void done() {
		
	}
}
