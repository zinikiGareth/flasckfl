package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;

public class AjaxCreator extends LeafAdapter {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final AjaxCreate ac;

	public AjaxCreator(JSClassCreator clz, JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner, AjaxCreate ac) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.ac = ac;
		clz.field(false, Access.PRIVATE, new PackageName(J.OBJECT), ac.var.baseName());
		block.setField(false, ac.var.baseName(), block.createAjax(runner, ac.baseUrl));
		sv.push(this);
	}
	
	@Override
	public void visitAjaxExpectSubscribe(AjaxSubscribe as) {
		new AjaxExpectSubscribe(state, sv, block, state.meth().member(new PackageName(J.AJAXMOCK), ac.var.baseName()), as);
	}

	@Override
	public void leaveAjaxCreate(AjaxCreate ac) {
		sv.result(null);
	}

}
