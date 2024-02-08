package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.parsedForm.ParenAt;

public class ParenExprConsumer implements Consumer<Expr>, ParenAt, Locatable {
	private final Consumer<Expr> delegate;
	private InputPosition where;

	public ParenExprConsumer(Consumer<Expr> delegate) {
		this.delegate = delegate;
		
	}
	@Override
	public void accept(Expr t) {
		where = t.location();
		delegate.accept(t);
	}

	@Override
	public void parenAt(InputPosition pos) {
		this.where = pos;
	}
	
	public InputPosition location() {
		return where;
	}
}
