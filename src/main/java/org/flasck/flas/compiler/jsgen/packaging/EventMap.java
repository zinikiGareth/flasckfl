package org.flasck.flas.compiler.jsgen.packaging;

import java.util.List;

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
				ft = writeHi(ft, lw, tt.type, tt.slot, tt.option, methods.getHandler(tt.handler));
			}
			if (!ft)
				kw.println("");
			kw.println("]");
			jw.newline();
		}
		List<HandlerInfo> unbound = methods.unboundHandlers();
		if (!unbound.isEmpty()) {
			if (!isFirst) {
				kw.println(",");
			} else
				kw.println("");
			kw.print("_: [");
			boolean ft = true;
			IndentWriter lw = kw.indent();
			for (HandlerInfo hi : unbound) {
				ft = writeHi(ft, lw, null, null, 0, hi);
			}
			if (!ft)
				kw.println("");
			kw.println("]");
			jw.newline();
		}
		jw.println("};");
		iw.println("};");
	}

	private boolean writeHi(boolean ft, IndentWriter lw, String type, String slot, int option, HandlerInfo hi) {
		if (!ft) {
			lw.println(",");
		} else
			lw.println("");
		lw.print("{ ");
		if (type != null) {
			lw.print("type: ");
			lw.print(new JSString(type).asVar());
			lw.print(", slot: ");
			lw.print(new JSString(slot).asVar());
			lw.print(", ");
			lw.print("option: ");
			lw.print(Integer.toString(option));
			lw.print(", ");
		}
		lw.print("event: ");
		lw.print(hi.event);
		lw.print(", handler: ");
		lw.print(hi.name.jsPName());
		lw.print(" }");
		return false;
	}
}
