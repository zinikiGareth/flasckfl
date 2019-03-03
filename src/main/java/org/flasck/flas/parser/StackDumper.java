package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;

public interface StackDumper {

	void dump(List<Expr> terms);

	StackDumper indent(int i);

	void levels(int size);

}
