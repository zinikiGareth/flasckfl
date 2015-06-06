package org.flasck.flas;

@SuppressWarnings("serial")
public class ResolutionException extends RuntimeException {
//	private String name;

	public ResolutionException(String name) {
		super("Could not resolve name " + name);
//		this.name = name;
	}
	
	

}
