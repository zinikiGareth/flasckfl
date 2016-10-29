package org.flasck.flas.vcode.hsieForm;

public interface PushVisitor {
	public Object visit(PushVar pv);
	public Object visit(PushInt pi);
	public Object visit(PushString ps);
	public Object visit(PushExternal pe);
	public Object visit(PushTLV pt);
	public Object visit(PushCSR pc);
	public Object visit(PushFunc pf);
}
