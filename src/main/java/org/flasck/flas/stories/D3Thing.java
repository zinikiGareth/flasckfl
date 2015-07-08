package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.D3PatternBlock;

public class D3Thing {
	public final String prefix;
	public final String name;
	public final InputPosition dloc;
	public final Object data;
	public final String iter;
	public final List<D3PatternBlock> patterns;

	public D3Thing(String prefix, String name, InputPosition dloc, Object data, String iter, List<D3PatternBlock> patterns) {
		this.prefix = prefix;
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
