package org.flasck.flas.repository;

import java.util.Set;

import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.tc3.Type;

public interface RepositoryReader {
	<T extends RepositoryEntry> T get(String string);
	void traverse(Visitor visitor);
	void dump();
	Type findUnionWith(Set<String> ms);
}
