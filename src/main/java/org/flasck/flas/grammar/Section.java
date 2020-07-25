package org.flasck.flas.grammar;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.xml.XMLElement;

public class Section {
	public final String title;
	public final String desc;
	private List<Production> productions = new ArrayList<>();

	public Section(String title, XMLElement desc) {
		this.title = title;
		StringBuilder sb = new StringBuilder();
		desc.serializeChildrenTo(sb);
		this.desc = sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Section && ((Section)obj).title.equals(this.title);
	}
	
	public int hashCode() {
		return title.hashCode();
	}
	
	public void add(Production theProd) {
		this.productions.add(theProd);
	}

	public Iterable<Production> productions() {
		return productions;
	}
}
