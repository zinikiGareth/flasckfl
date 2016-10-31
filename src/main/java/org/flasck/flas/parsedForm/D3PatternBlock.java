package org.flasck.flas.parsedForm;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;

public class D3PatternBlock {
	public final InputPosition kw;
	public final StringLiteral pattern;
	public final Map<String, D3Section> sections = new TreeMap<String, D3Section>();

	public D3PatternBlock(InputPosition kw, StringLiteral pattern) {
		this.kw = kw;
		this.pattern = pattern;
	}
}
