package org.flasck.flas.parsedForm;

public class CardFunction implements ExternalRef {
	public final String clzName;
	public final String function;
	public final boolean fromHandler;

	public CardFunction(String clzName, String function) {
		this.clzName = clzName;
		this.function = function;
		this.fromHandler = false;
	}
	
	public CardFunction(CardFunction inner, boolean fromHandler) {
		this.clzName = inner.clzName;
		this.function = inner.function;
		this.fromHandler = fromHandler;
	}

	public String uniqueName() {
		return this.clzName + "." + this.function;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	public boolean fromHandler() {
		return fromHandler;
	}
	
	@Override
	public String toString() {
		return this.clzName + "." + this.function;
	}
}
