package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestMatch;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.HaventConsideredThisException;

public class DoUTMatchGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private final List<JSExpr> args = new ArrayList<>();
	private static Iterable<MatchGeneratorModule> modules = null;

	static  {
		modules = ServiceLoader.load(MatchGeneratorModule.class);
	}

	public DoUTMatchGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
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
	public void leaveUnitTestMatch(UnitTestMatch m) {
		String freeText = m.text == null ? "" : m.text.text();
		theswitch:
		switch (m.what.item()) {
		case "text":
			block.assertable(runner, "matchText", args.get(0), DoUTEventGeneratorJS.makeSelector(block, m.targetZone), block.literal(Boolean.toString(m.contains)), block.literal(Boolean.toString(m.fails)), block.string(freeText));
			break;
		case "style":
			block.assertable(runner, "matchStyle", args.get(0), DoUTEventGeneratorJS.makeSelector(block, m.targetZone), block.literal(Boolean.toString(m.contains)), block.string(freeText));
			break;
		case "scroll":
			block.assertable(runner, "matchScroll", args.get(0), DoUTEventGeneratorJS.makeSelector(block, m.targetZone), block.literal(Boolean.toString(m.contains)), block.literal(Double.toString(Double.parseDouble(freeText))));
			break;
		case "image_uri":
			block.assertable(runner, "matchImageUri", args.get(0), DoUTEventGeneratorJS.makeSelector(block, m.targetZone), block.string(freeText));
			break;
		case "href":
			block.assertable(runner, "matchHref", args.get(0), DoUTEventGeneratorJS.makeSelector(block, m.targetZone), block.string(freeText));
			break;
		default: {
			for (MatchGeneratorModule mod : modules) {
				if (mod.generateMatch(runner, block, args.get(0), m, freeText))
					break theswitch;
			}
			throw new HaventConsideredThisException("cannot handle match " + m);
		}
		}
		sv.result(null);
	}
}
