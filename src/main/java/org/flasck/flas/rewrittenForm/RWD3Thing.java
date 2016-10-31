package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateLine;
import org.flasck.flas.parsedForm.D3Intro;

public class RWD3Thing implements TemplateLine {
	public final String prefix;
	public final InputPosition nameLoc;
	public final String name;
	public final InputPosition varLoc;
	public final Object data;
	public final String iterVar;
	public final List<RWD3PatternBlock> patterns;

	public RWD3Thing(String prefix, D3Intro d3, Object expr, List<RWD3PatternBlock> patterns) {
		int idx = prefix.lastIndexOf(".");
		if (prefix.charAt(idx+1) == '_')
			this.prefix = prefix;
		else
			this.prefix = prefix.substring(0, idx+1) + "_" + prefix.substring(idx+1);
		this.nameLoc = d3.nameLoc;
		this.name = d3.name;
		this.data = expr;
		this.varLoc = d3.varLoc;
		this.iterVar = d3.iterVar;
		this.patterns = patterns;
	}

	@Override
	public String toString() {
		return "D3Thing[" + name + "," + data + "," + iterVar + "]";
	}
}
