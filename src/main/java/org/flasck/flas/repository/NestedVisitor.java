package org.flasck.flas.repository;

import org.flasck.flas.repository.Repository.Visitor;

public interface NestedVisitor extends Visitor {

	void push(Visitor v);

	void result(Object r); // should this be a parameterized type or does that just get complicated?

}
