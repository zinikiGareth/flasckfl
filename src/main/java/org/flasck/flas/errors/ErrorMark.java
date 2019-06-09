package org.flasck.flas.errors;

public interface ErrorMark {
	public boolean hasMore();
	public boolean contains(FLASError e);
}