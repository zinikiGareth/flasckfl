package test.droidgen;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public final class ReturnNewVar implements Action {
	private MethodDefiner meth;
	private String clz;
	private String name;

	public ReturnNewVar(MethodDefiner meth, String clz, String name) {
		this.meth = meth;
		this.clz = clz;
		this.name = name;
	}
	
	@Override
	public void describeTo(Description desc) {
		desc.appendText("return a new avar for this");
	}

	@Override
	public Object invoke(Invocation invoke) throws Throwable {
		return new Var.AVar(meth, clz, name);
	}
}