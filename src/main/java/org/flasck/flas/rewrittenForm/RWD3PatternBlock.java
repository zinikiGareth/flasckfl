package org.flasck.flas.rewrittenForm;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.StringLiteral;

public class RWD3PatternBlock {
	public final StringLiteral pattern;
	public final Map<String, RWD3Section> sections = new TreeMap<String, RWD3Section>();

	public RWD3PatternBlock(StringLiteral pattern) {
		this.pattern = pattern;
	}
}
