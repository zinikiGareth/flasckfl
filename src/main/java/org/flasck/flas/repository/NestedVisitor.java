package org.flasck.flas.repository;

public interface NestedVisitor extends RepositoryVisitor, ResultAware {
	RepositoryVisitor top();
	void push(RepositoryVisitor v);
}
