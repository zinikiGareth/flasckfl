package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TargetZone.Qualifier;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoUTEventGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private final List<JSExpr> args = new ArrayList<>();

	public DoUTEventGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
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
	public void leaveUnitTestEvent(UnitTestEvent e) {
		if (args.size() != 2)
			throw new RuntimeException("expected card & event");
		// TODO: this needs to be an array of [type, name] elements to traverse the tree
		block.assertable(runner, "event", args.get(0), makeSelector(block, e.targetZone), args.get(1));
		sv.result(null);
	}

	public static JSExpr makeSelector(JSBlockCreator block, TargetZone targetZone) {
		List<JSExpr> al = new ArrayList<JSExpr>();
		for (int i=0;i<targetZone.fields.size();i++) {
			String ty = targetZone.types().get(i).toString().toLowerCase();
			JSExpr je = makeEventZone(block, targetZone.fields.get(i));
			if (je != null)
				al.add(block.makeEventZone(block.string(ty), je));
		}
		return block.makeArray(al);
	}

	public static JSExpr makeEventZone(JSBlockCreator block, Object o) {
		if (o instanceof String)
			return block.string((String)o);
		else if (o instanceof Qualifier)
			return null;
		else
			return block.literal(Integer.toString((Integer)o));
	}
}
