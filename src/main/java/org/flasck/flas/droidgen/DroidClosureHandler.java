package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.generators.GenerationContext;
import org.flasck.flas.hsie.ObjectNeeded;
import org.flasck.flas.hsie.PushArgumentTraverser;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.OutputHandler;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;

public class DroidClosureHandler implements ClosureHandler<IExpr> {
	private final NewMethodDefiner meth;
	private final GenerationContext<IExpr> cxt;
	
	public DroidClosureHandler(GenerationContext<IExpr> cxt) {
		this.cxt = cxt;
		this.meth = cxt.getMethod();
	}

	@Override
	public void visit(HSIEForm form, PushReturn expr) {
		expr.visit(new PushArgumentTraverser<IExpr>(form, cxt), new OutputHandler<IExpr>() {
			@Override
			public void result(IExpr expr) {
				cxt.closureArg(expr);
			}
		});
	}

	@Override
	public void beginClosure() {
		cxt.beginClosure();
	}

	@Override
	public ClosureHandler<IExpr> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
		return new ClosureHandler<IExpr>() {
			private List<IExpr> al = new ArrayList<>();
			private List<IExpr> vas = new ArrayList<>();
			
			@Override
			public void beginClosure() {
				if (on == ObjectNeeded.THIS)
					al.add(meth.myThis());
				else if (on == ObjectNeeded.CARD)
					al.add(meth.getField("_card"));
				al.add(meth.classConst(clz.javaClassName()));
				al.add(meth.intConst(arity));
			}
			
			@Override
			public void visit(HSIEForm form, PushReturn expr) {
				expr.visit(new PushArgumentTraverser<IExpr>(form, cxt), new OutputHandler<IExpr>() {
					@Override
					public void result(IExpr expr) {
						vas.add(meth.box(expr));
					}
				});
			}
			
			@Override
			public ClosureHandler<IExpr> curry(NameOfThing clz, ObjectNeeded on, Integer arity) {
				throw new org.zinutils.exceptions.NotImplementedException();
			}
			
			@Override
			public void endClosure(OutputHandler<IExpr> handler) {
				al.add(meth.arrayOf(J.OBJECT, vas));
				IExpr[] args = new IExpr[al.size()];
				al.toArray(args);
				handler.result(meth.makeNew(J.FLCURRY, args));
			}
		};
	}

	@Override
	public void endClosure(OutputHandler<IExpr> handler) {
		handler.result(cxt.endClosure());
	}
}
