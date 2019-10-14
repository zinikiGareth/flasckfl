package org.flasck.flas.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.patterns.HSIOptions;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class GroupChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final List<Type> types = new ArrayList<>();
	private Type exprType;
	private CurrentTCState state;
	private List<Slot> slots;

	public GroupChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		System.out.println("TC fn " + fn.name().uniqueName());
		types.clear();
		this.slots = fn.slots();
	}
	
	@Override
	public void visitFunctionIntro(FunctionIntro fi) {
		sv.push(new ExpressionChecker(errors, repository, state, sv));
		visitPatterns(fi.hsiTree());
	}
	
	// TODO: it feels weird to do this here - it belongs in Traverser ...
	// Yet another mode?
	public void visitPatterns(HSITree hsiTree) {
		for (int i=0;i<hsiTree.width();i++) {
			visitSlot(this.slots.get(i), hsiTree.get(i));
		}
	}

	private void visitSlot(Slot slot, HSIOptions hsiOptions) {
		state.argType(hsiOptions.minimalType(state, repository));
	}

	@Override
	public void result(Object r) {
		this.exprType = (Type) r;
	}

	@Override
	public void leaveFunctionIntro(FunctionIntro fi) {
		HSITree tree = fi.hsiTree();
		if (tree.width() == 0)
			types.add(exprType);
		else {
			Type[] atypes = new Type[tree.width() + 1];
			for (int i=0;i<tree.width();i++) {
				atypes[i] = tree.get(i).minimalType(state, repository);
			}
			atypes[atypes.length-1] = exprType;
			types.add(new Apply(atypes));
		}
		this.slots = null;
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		if (fn.intros().isEmpty())
			return;
		if (types.isEmpty())
			throw new RuntimeException("No types inferred for " + fn.name().uniqueName());
		System.out.println("TC fn " + fn.name().uniqueName() + " = " + consolidateType());
		fn.bindType(consolidateType());
		types.clear();
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		System.out.println("Leave TC grp " + grp);
		sv.result(null);
	}

	private Type consolidateType() {
		// TODO: this actually needs to consolidate the types ...
		return types.get(0);
	}
}
