package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ut.UnitTestRender;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoUTRenderGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private final List<JSExpr> args = new ArrayList<>();

	public DoUTRenderGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, true);
	}
	
	@Override
	public void result(Object r) {
		args.add((JSExpr)r);
	}
	
	@Override
	public void leaveUnitTestRender(UnitTestRender r) {
		if (args.size() != 1)
			throw new RuntimeException("expected object");
		UnitDataDeclaration udd = (UnitDataDeclaration) r.card.defn();
		ObjectDefn od = (ObjectDefn) udd.ofType.defn();
		block.assertable(runner, "render", block.nameOf(args.get(0)), block.literal(od.name().jsName()+".prototype._updateTemplate" + Integer.toString(r.template.template().position())), block.string("items/" + r.template.template().name().baseName()));
		sv.result(null);
	}
}
