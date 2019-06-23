package org.flasck.flas.repository;

import org.flasck.flas.repository.Repository.Visitor;

public interface RepositoryReader {
	<T extends RepositoryEntry> T get(String string);
	void traverse(Visitor visitor);
}
