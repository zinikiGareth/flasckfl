package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;

public interface SystemTestModule {

	void inject(JSMethodCreator meth, JSFunctionState state, JSBlockCreator block, JSExpr runner);

}
