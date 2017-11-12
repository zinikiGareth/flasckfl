package org.flasck.flas.jsform;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushReturn;

public class JSCurryHandler implements ClosureHandler<String> {
	private final HSIEForm form;

	public JSCurryHandler(HSIEForm form) {
		this.form = form;
	}

	@Override
	public void beginClosure() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void visit(PushReturn expr) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public ClosureHandler<String> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
		return new ClosureHandler<String>() {
			StringBuilder sb = new StringBuilder();
			
			@Override
			public void beginClosure() {
				if (on == ObjectNeeded.CARD)
					sb.append("FLEval.oclosure(this._card, ");
				else if (on == ObjectNeeded.THIS)
					sb.append("FLEval.oclosure(this, ");
				else
					sb.append("FLEval.closure(");
				sb.append("FLEval.curry, ");
				if (clz instanceof FunctionName)
					sb.append(((FunctionName)clz).jsSPname());
				else if (clz instanceof VarName)
					sb.append(clz.uniqueName());
				sb.append(", ");
				sb.append(arity);
			}

			@Override
			public void visit(PushReturn expr) {
				sb.append(", ");
				JSForm.appendValue(form, sb, expr, -27);
			}
			
			@Override
			public ClosureHandler<String> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
				throw new org.zinutils.exceptions.NotImplementedException();
			}
			
			@Override
			public void endClosure(OutputHandler<String> handler) {
				sb.append(")");
				handler.result(sb.toString());
			}
		};
	}

	@Override
	public void endClosure(OutputHandler<String> handler) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}