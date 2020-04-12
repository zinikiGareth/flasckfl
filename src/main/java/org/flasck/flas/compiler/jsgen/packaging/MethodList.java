package org.flasck.flas.compiler.jsgen.packaging;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.bytecode.mock.IndentWriter;

public class MethodList {
	private final NameOfThing name;
	private final List<FunctionName> methods;

	public MethodList(NameOfThing name, List<FunctionName> methods) {
		this.name = name;
		this.methods = methods;
	}

	public void write(IndentWriter iw) {
		iw.println(name.jsName() + ".prototype.methods = function() {");
		IndentWriter jw = iw.indent();
		jw.print("return {");
		IndentWriter kw = jw.indent();
		boolean isFirst = true;
		for (FunctionName f : methods) {
			if (!isFirst) {
				kw.print(",");
			}
			isFirst = false;
			kw.println("");
			kw.print("\"" + f.name + "\": " + f.jsPName());
		}
		jw.println("");
		jw.println("};");
		iw.println("};");
	}

}
