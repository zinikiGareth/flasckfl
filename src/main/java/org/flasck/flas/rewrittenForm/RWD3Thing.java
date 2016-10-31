package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateLine;

public class RWD3Thing implements TemplateLine {
	public final String prefix;
	public final String name;
	public final InputPosition dloc;
	public final Object data;
	public final String iter;
	public final List<RWD3PatternBlock> patterns;

	public RWD3Thing(String prefix, String name, InputPosition dloc, Object data, String iter, List<RWD3PatternBlock> patterns) {
		int idx = prefix.lastIndexOf(".");
		if (prefix.charAt(idx+1) == '_')
			this.prefix = prefix;
		else
			this.prefix = prefix.substring(0, idx+1) + "_" + prefix.substring(idx+1);
		this.name = name;
		this.dloc = dloc;
		this.data = data;
		this.iter = iter;
		this.patterns = patterns;
	}

	@Override
	public String toString() {
		return "D3Thing[" + name + "," + data + "," + iter + "]";
	}
}
