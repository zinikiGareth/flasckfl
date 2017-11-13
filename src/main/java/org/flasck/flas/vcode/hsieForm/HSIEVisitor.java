package org.flasck.flas.vcode.hsieForm;

public interface HSIEVisitor<T> {
	void visit(Head h);
	void visit(Switch sw);
	void visit(BindCmd n);
	void visit(IFCmd n);
	void visit(PushReturn pr);
	void visit(ErrorCmd n);
	T done();
}
