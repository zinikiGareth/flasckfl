package org.flasck.flas.parsedForm;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.StringLiteral;

public class D3PatternBlock {
	public final StringLiteral pattern;
	public final Map<String, D3Section> sections = new TreeMap<String, D3Section>();

	public D3PatternBlock(StringLiteral pattern) {
		this.pattern = pattern;
	}
}
