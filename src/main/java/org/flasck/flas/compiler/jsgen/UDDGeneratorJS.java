package org.flasck.flas.compiler.jsgen;

import java.util.List;
import java.util.Set;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.AgentDefinition;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.PolyInstance;

public class UDDGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSMethodCreator meth;
	private final JSFunctionState state;
	private final JSBlockCreator block;
	private JSExpr assigned;
	private boolean assigning;

	public UDDGeneratorJS(NestedVisitor sv, JSMethodCreator meth, JSFunctionState state, JSBlockCreator block) {
		this.sv = sv;
		this.meth = meth;
		this.state = state;
		this.block = block;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false);
	}
	
	@Override
	public void visitUnitDataField(Assignment assign) {
		assigning = true;
	}
	
	@Override
	public void result(Object r) {
		if (!assigning)
			this.assigned = (JSExpr) r;
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		JSExpr value;
		if (assigned != null) {
			value = assigned;
		} else {
			value = meth.createObject(udd.ofType.defn().name());
		}
		JSExpr newMock = block.storeMockObject(udd, value);
		// I think this is where we would then want to do the assigning of fields ...
		state.addMock(udd, newMock);
		
		sv.result(null);
	}

	public static void handleUDD(NestedVisitor sv, JSMethodCreator meth, JSFunctionState state, JSBlockCreator block, Set<UnitDataDeclaration> globalMocks, List<JSExpr> explodingMocks, UnitDataDeclaration udd) {
		if (meth == null) {
			globalMocks.add(udd);
			return;
		}
		NamedType objty = udd.ofType.defn();
		if (objty instanceof PolyInstance)
			objty = ((PolyInstance)objty).struct();
		if (objty instanceof ContractDecl) {
			ContractDecl cd = (ContractDecl) objty;
			JSExpr mock;
			if (cd.type == ContractType.HANDLER) {
				if (udd.expr != null) {
					new UDDGeneratorJS(sv, meth, state, block);
					return;
				}
				mock = meth.mockHandler((SolidName) objty.name());
			} else
				mock = meth.mockContract((SolidName) objty.name());
			state.addMock(udd, mock);
			explodingMocks.add(mock);
		} else if (objty instanceof AgentDefinition) {
			JSExpr obj = meth.createAgent((CardName) objty.name());
			state.addMock(udd, obj);
		} else if (objty instanceof CardDefinition) {
			JSExpr obj = meth.createCard((CardName) objty.name());
			state.addMock(udd, obj);
		} else if (objty instanceof ServiceDefinition) {
			JSExpr obj = meth.createService((CardName) objty.name());
			state.addMock(udd, obj);
		} else if (objty instanceof StructDefn || objty instanceof UnionTypeDefn) {
			new UDDGeneratorJS(sv, meth, state, block);
		} else if (objty instanceof ObjectDefn) {
			new UDDGeneratorJS(sv, meth, state, block);
		} else if (objty instanceof HandlerImplements) {
			new UDDGeneratorJS(sv, meth, state, block);
		} else {
			/* It seems to me that this requires us to traverse the whole of 
			 * the inner expression.  I'm not quite sure what is the best way to handle that.
			 * Another option on the traverser? A signal back to the traverser (how?) that
			 * says "traverse this"?  Creating a subtraverser here?
			 * 
			 * Reviewing this today, I don't see why you wouldn't want to traverse it all the time
			 * But probably have individual visit/leave combos for uddExpr and each uddField
			 * All ended by leaveUDD
			 */
			throw new RuntimeException("not handled: " + objty + " of " + objty.getClass());
		}
	}
}
