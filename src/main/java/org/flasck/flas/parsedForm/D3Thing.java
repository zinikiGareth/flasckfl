package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.template.TemplateLine;

public class D3Thing implements TemplateLine {
	public final D3Intro d3;
	// TODO: D3
//	public final String prefix;
//	public final String name;
//	public final InputPosition dloc;
//	public final Object data; // now expr
//	public final String iter; // now iterVar
	public final List<D3PatternBlock> patterns;

	public D3Thing(D3Intro d3, List<D3PatternBlock> patterns) {
		this.d3 = d3;
		/* TODO: D3
		int idx = prefix.lastIndexOf(".");
		if (prefix.charAt(idx+1) == '_')
			this.prefix = prefix;
		else
			this.prefix = prefix.substring(0, idx+1) + "_" + prefix.substring(idx+1);
		*/
//		this.name = name;
//		this.dloc = dloc;
//		this.data = data;
//		this.iter = iter;
		this.patterns = patterns;
	}

	@Override
	public String toString() {
		return "D3Thing[" + d3.name + "," + d3.expr + "," + d3.iterVar + "]";
	}
}
