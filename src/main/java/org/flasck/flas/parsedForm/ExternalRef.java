package org.flasck.flas.parsedForm;

public interface ExternalRef extends Locatable, Comparable<Object>{
	public String uniqueName();

	public boolean fromHandler();
}
