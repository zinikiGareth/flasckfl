package org.flasck.flas.compiler.jsgen.packaging;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.web.EventTargetZones;
import org.flasck.flas.web.EventPlacement.HandlerInfo;
import org.flasck.flas.web.EventPlacement.TemplateTarget;
import org.zinutils.bytecode.mock.IndentWriter;

public class EventMap {
	private final NameOfThing cardName;
	private final EventTargetZones methods;

	public EventMap(NameOfThing cardName, EventTargetZones eventMethods) {
		this.cardName = cardName;
		this.methods = eventMethods;
	}

	public void write(IndentWriter iw) {
		{
			// rework this as:
			//   a function returning
			//     a map of template names to
			//       a list of 'handlers' for that template, each handler being:
			//         an area type (content or style)
			//         an area name (eg message)
			//         an FLEventType (eg ClickEvent)
			//         -- and I think I want the handler here too ...
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
		// First write out the class objects
		{
			iw.println(cardName.jsName() + ".prototype._eventClasses = function() {");
			IndentWriter jw = iw.indent();
			jw.print("return [");
			boolean isFirst = true;
			for (String ev : methods.handledEvents()) {
				if (!isFirst) {
					jw.print(",");
				}
				isFirst = false;
				jw.print(ev);
			}
			jw.println("];");
			iw.println("};");
		}
		
		// Now give the handler mapping
		{
			iw.println(cardName.jsName() + ".prototype._events = function() {");
			IndentWriter jw = iw.indent();
			jw.print("return {");
			IndentWriter kw = jw.indent();
			boolean isFirst = true;
			for (String ev : methods.handledEvents()) {
				if (!isFirst) {
					kw.print(",");
				}
				isFirst = false;
				kw.println("");
				kw.print("\"" + ev + "\": [");
				for (FunctionName f : methods.handling(ev))
					kw.print(f.jsPName());
				kw.print("]");
			}
			jw.println("");
			jw.println("};");
			iw.println("};");
		}
	}
}
