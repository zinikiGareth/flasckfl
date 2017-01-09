package org.flasck.flas.newtypechecker;

@SuppressWarnings("serial")
public class NeedIndirectionException extends Exception {

	public final String name;

	public NeedIndirectionException(String name) {
		this.name = name;
	}

	@Override
	public String getMessage() {
		return "the name '" + name + "' cannot be resolved for typechecking";
	}
}
