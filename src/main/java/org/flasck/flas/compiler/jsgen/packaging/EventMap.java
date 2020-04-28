package org.flasck.flas.compiler.jsgen.packaging;

import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.bytecode.mock.IndentWriter;

public class EventMap {
	private final NameOfThing name;
	private final Map<String, FunctionName> methods;

	public EventMap(NameOfThing name, Map<String, FunctionName> eventMethods) {
		this.name = name;
		this.methods = eventMethods;
	}

	public void write(IndentWriter iw) {
		// First write out the class objects
		{
			iw.println(name.jsName() + ".prototype._eventClasses = function() {");
			IndentWriter jw = iw.indent();
			jw.print("return [");
			boolean isFirst = true;
			for (Entry<String, FunctionName> f : methods.entrySet()) {
				if (!isFirst) {
					jw.print(",");
				}
				isFirst = false;
				jw.print(f.getKey());
			}
			jw.println("];");
			iw.println("};");
		}
		
		// Now give the handler mapping
		{
			iw.println(name.jsName() + ".prototype._events = function() {");
			IndentWriter jw = iw.indent();
			jw.print("return {");
			IndentWriter kw = jw.indent();
			boolean isFirst = true;
			for (Entry<String, FunctionName> f : methods.entrySet()) {
				if (!isFirst) {
					kw.print(",");
				}
				isFirst = false;
				kw.println("");
				kw.print("\"" + f.getKey() + "\": " + f.getValue().jsPName());
			}
			jw.println("");
			jw.println("};");
			iw.println("};");
		}
	}
}
