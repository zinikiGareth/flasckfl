package org.flasck.flas.errors;

public interface ErrorMark {
	public boolean hasMoreNow();
	public boolean contains(FLASError e);
}