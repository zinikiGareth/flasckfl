package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;

public class AjaxCreator {

	public AjaxCreator(JSClassCreator clz, JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner, AjaxCreate ac) {
		clz.field(false, Access.PRIVATE, new PackageName(J.OBJECT), ac.var.baseName());
		block.setField(false, ac.var.baseName(), block.createAjax(runner, ac.baseUrl));
	}

}
