package org.flasck.flas.repository;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.Traverser.VarMapping;
import org.zinutils.exceptions.InvalidUsageException;

public class BackupPlan {
	private final VarMapping vars;
	private final String indent;
	private final List<Slot> slots;
	private final List<FunctionIntro> intros = new ArrayList<>();

	public BackupPlan() {
		this.vars = null;
		this.indent = null;
		this.slots = null;
	}

	public BackupPlan(VarMapping vars, String indent, List<Slot> slots) {
		this.vars = vars;
		this.indent = indent;
		this.slots = slots;
	}
	
	public boolean hasNone() {
		return intros.isEmpty();
	}

	public int size() {
		return intros.size();
	}

	public boolean hasHope() {
		return !intros.isEmpty();
	}

	public FunctionIntro singleton() {
		if (intros.size() != 1)
			throw new InvalidUsageException("only call singleton when you have one intro and no more slots");
		return intros.get(0);
	}
	
	public void backup(Traverser traverser, DontConsiderAgain dca) {
		Traverser.hsiLogger.info(indent + "invoking backup plan with " + slots + " " + dca + " " + vars);
		traverser.visitHSI(vars, indent, slots, new FunctionHSICases(intros), null, new BackupPlan(), dca);
	}

	@Override
	public String toString() {
		return "Slots" + (slots == null ? "[]": slots.toString()) + " " + intros.size();
		
	}

	public void allows(List<FunctionIntro> addMore) {
		for (FunctionIntro fi : addMore)
			if (!intros.contains(fi))
				intros.add(fi);
	}
}
