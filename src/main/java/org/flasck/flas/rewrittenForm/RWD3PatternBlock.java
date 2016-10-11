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

	/*
	public final List<Object> contents;
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<Object> formats;
	public final List<D3Line> nested = new ArrayList<D3Line>();
	public final List<EventHandler> handlers = new ArrayList<EventHandler>();

	public D3Line(List<Object> contents, String customTag, String customTagVar, List<Object> attrs, List<Object> formats) {
		this.contents = contents;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
		this.formats = formats;
	}

	public boolean isDiv() {
		if (contents.isEmpty())
			return true;
		if (contents.size() > 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateToken && ((TemplateToken)o).type == TemplateToken.DIV;
	}

	public boolean isList() {
		if (contents.size() != 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateList;
	}

	public boolean isTemplate() {
		if (contents.size() != 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateReference;
	}

	public boolean isCases() {
		if (contents.size() != 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateCases;
	}

	public boolean isOr() {
		if (contents.size() != 1)
			return false;
		Object o = contents.get(0);
		return o != null && o instanceof TemplateOr;
	}
	*/
}
