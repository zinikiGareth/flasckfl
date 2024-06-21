package org.flasck.flas.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.FunctionConstness;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.FunctionGroups;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Traverser;

public class FigureFunctionConstness extends LeafAdapter {
	private final Traverser traverser;
	private boolean needsState;
	private List<NameOfThing> depends;

	public FigureFunctionConstness() {
		traverser = new Traverser(this);
	}

	public void processAll(FunctionGroups ordering) {
		// 1. Traverse all the functions in their groups, identifying ones with arguments and/or using state
		
		Map<String, FunctionConstness> map = new TreeMap<>();
		for (FunctionGroup grp : ordering) {
			for (LogicHolder f : grp.functions()) {
				map.put(f.name().uniqueName(), process(f));
			}
		}
		// debug: show what we've found
//		for (Entry<String, FunctionConstness> e : map.entrySet()) {
//			System.out.println(e.getKey() + " ==> " + e.getValue());
//		}
		
		// 2. Resolve dependencies & then set on the function
		// There are three possible outcomes from the above:
		// a. clearly constant
		// b. clearly not (uses state or args)
		// c. unclear because it has dependencies on other things
		// For case (c), we can look at all the dependencies, and follow them transitively, with the caveat
		// that we handle mutual dependencies by just stopping; if we don't see any "clearly not" dependencies,
		// then it must be constant.
		for (FunctionGroup grp : ordering) {
			for (LogicHolder f : grp.functions()) {
				String name = f.name().uniqueName();
				FunctionConstness fc = map.get(name);
				if (fc.depends != null) {
					if (hasStateDependencies(map, new ArrayList<>(), fc.depends))
						fc = new FunctionConstness("dependencies use state");
				}
				f.setConstness(fc);
//				System.out.println("final " + f + " ==> " + f.constNess());
			}
		}
	}
	
	private boolean hasStateDependencies(Map<String, FunctionConstness> map, List<String> already, List<NameOfThing> tocheck) {
		for (NameOfThing n : tocheck) {
			String name = n.uniqueName();
			if (already.contains(name))
				return false;
			already.add(name);
			FunctionConstness fc = map.get(name);
			if (fc == null) 
				; // it is imported and thus connot depend on our state
			else if (fc.reason != null)
				return true;
			else if (fc.depends != null) {
				if (hasStateDependencies(map, already, tocheck))
					return true;
			}
		}
		return false;
	}

	public FunctionConstness process(LogicHolder f) {
		needsState = false;
		depends = new ArrayList<>();
		if (f.hasArgs())
			return new FunctionConstness("hasArgs");
		if (f.hasState()) {
//			System.out.println("checking state for " + f.name().uniqueName());
			traverser.visitLogic(f);
			if (needsState)
				return new FunctionConstness("needsState");
		}
		return new FunctionConstness(depends);
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var, int nargs) {
		if (var.defn() instanceof StructField) {
//			System.out.println("  depends on object field " + var.var);
			needsState = true;
		} else if (var.defn() instanceof LogicHolder) {
//			System.out.println("  has dependency on logic " + var.defn().name().uniqueName());
			depends.add(var.defn().name());
		}
	}
}
