package org.flasck.flas.repository;

public interface NestedVisitor extends RepositoryVisitor, ResultAware {
	void push(RepositoryVisitor v);
}
