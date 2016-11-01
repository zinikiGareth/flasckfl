package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;

public class D3PatternBlock implements Comparable<D3PatternBlock> {
	public final InputPosition kw;
	public final StringLiteral pattern;
	public final List<D3Section> sections = new ArrayList<D3Section>();

	public D3PatternBlock(InputPosition kw, StringLiteral pattern) {
		this.kw = kw;
		this.pattern = pattern;
	}

	@Override
	public int compareTo(D3PatternBlock o) {
		return pattern.text.compareTo(o.pattern.text);
	}
}
