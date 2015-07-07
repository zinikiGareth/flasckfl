package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.parsedForm.D3PatternBlock;

public class D3Thing {
	public final String prefix;
	public final String name;
	public final List<D3PatternBlock> patterns;

	public D3Thing(String prefix, String name, List<D3PatternBlock> patterns) {
		this.prefix = prefix;
		this.name = name;
		this.patterns = patterns;
	}

	@Override
	public String toString() {
		return "D3Thing[" + name + "]";
	}
}
