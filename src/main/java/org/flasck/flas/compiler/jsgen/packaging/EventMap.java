package org.flasck.flas.compiler.jsgen.packaging;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.templates.EventPlacement.HandlerInfo;
import org.flasck.flas.compiler.templates.EventPlacement.TemplateTarget;
import org.flasck.flas.compiler.templates.EventTargetZones;
import org.zinutils.bytecode.mock.IndentWriter;

public class EventMap {
	private final NameOfThing cardName;
	private final EventTargetZones methods;

	public EventMap(NameOfThing cardName, EventTargetZones eventMethods) {
		this.cardName = cardName;
		this.methods = eventMethods;
	}

	public void write(IndentWriter iw) {
		iw.println(cardName.jsName() + ".prototype._eventHandlers = function() {");
		IndentWriter jw = iw.indent();
		jw.print("return {");
		IndentWriter kw = jw.indent();
		boolean isFirst = true;
		for (String t : methods.templateNames()) {
			if (!isFirst) {
				kw.println(",");
			} else
				kw.println("");
			
			isFirst = false;
			kw.print(new JSString(t).asVar());
			kw.print(" : [");
			IndentWriter lw = kw.indent();
			boolean ft = true;
			for (TemplateTarget tt : methods.targets(t)) {
				if (!ft) {
					lw.println(",");
				} else
					lw.println("");
				HandlerInfo hi = methods.getHandler(tt.handler);
				ft = false;
				lw.print("{ type: ");
				lw.print(new JSString(tt.type).asVar());
				lw.print(", slot: ");
				lw.print(new JSString(tt.slot).asVar());
				lw.print(", event: ");
				lw.print(hi.event);
				lw.print(", handler: ");
				lw.print(hi.name.jsPName());
				lw.print(" }");
			}
			if (!ft)
				kw.println("");
			kw.println("]");
			jw.newline();
		}
		jw.println("};");
		iw.println("};");
	}
}
