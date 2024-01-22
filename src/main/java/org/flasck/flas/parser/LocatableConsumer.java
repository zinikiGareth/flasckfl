package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;

@FunctionalInterface
public interface LocatableConsumer<T> {
	public void accept(InputPosition pos, T obj);
}
