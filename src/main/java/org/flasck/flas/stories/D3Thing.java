package org.flasck.flas.stories;

import java.util.List;

import org.flasck.flas.parsedForm.D3Intro;
import org.flasck.flas.parsedForm.D3PatternBlock;

public class D3Thing {
	public final String name;
	public List<D3PatternBlock> patterns;

	public D3Thing(D3Intro d3, List<D3PatternBlock> lines) {
		this.patterns = lines;
		this.name = d3.name;
	}

	@Override
	public String toString() {
		return "D3Thing[" + name + "]";
	}
}
