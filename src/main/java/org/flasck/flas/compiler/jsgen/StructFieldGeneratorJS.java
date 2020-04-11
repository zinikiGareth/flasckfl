package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class StructFieldGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final String fieldName;
	private JSBlockCreator currentBlock;
	private JSExpr ret;

	public StructFieldGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator currentBlock, String fieldName, JSExpr evalRet) {
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.fieldName = fieldName;
//		this.meth = state.meth;
		this.ret = evalRet;
		sv.push(this);
		new ExprGeneratorJS(state, sv, currentBlock, false);
	}

	@Override
	public void result(Object r) {
		currentBlock.storeField(ret, fieldName, (JSExpr) r);
		sv.result(null);
	}
}
