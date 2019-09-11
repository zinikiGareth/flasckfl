package org.flasck.flas.patterns;

public class HSIPatternOptions implements HSIOptions {
	// TODO: this needs to be MUCH more complex
	private HSITree nested;

	@Override
	public void addCM(String ctor, HSITree nested) {
		this.nested = nested;
	}

	@Override
	public HSITree getCM(String constructor) {
		return nested;
	}

}
