package org.flasck.flas.repository;

public interface RepositoryReader {

	<T extends RepositoryEntry> T get(String string);

}
