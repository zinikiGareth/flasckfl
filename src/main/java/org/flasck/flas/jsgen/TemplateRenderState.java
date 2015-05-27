package org.flasck.flas.jsgen;

public class TemplateRenderState {
	public final String name;
	private int lineNo = 1;

	public TemplateRenderState(String name) {
		this.name = name;
	}
	
	public int lineNo() {
		return lineNo++;
	}
}
