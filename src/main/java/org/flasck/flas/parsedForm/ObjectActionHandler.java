package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.lifting.NestedVarReader;
import org.flasck.flas.parsedForm.ut.GuardedMessages;
import org.flasck.flas.parser.MethodMessagesConsumer;
import org.flasck.flas.patterns.HSITree;
import org.flasck.flas.repository.FunctionHSICases;
import org.flasck.flas.repository.HSICases;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public abstract class ObjectActionHandler extends ObjectMessagesHolder implements Locatable, MethodMessagesConsumer, GuardedMessagesConsumer, RepositoryEntry, LogicHolder, PatternsHolder, TypeBinder {
	private static final Logger logger = LoggerFactory.getLogger("ObjectAH");
	private final InputPosition location;
	private final FunctionName name;
	private final List<Pattern> args;
	public final List<GuardedMessages> guards = new ArrayList<>();
	private HSITree hsiTree;
	private Type type;
	private List<FunctionIntro> convertedIntros;
	private NestedVarReader nestedVars;
	private boolean reportHolder;
	private boolean dynamicType = false;
	private FunctionConstness constNess;

	public ObjectActionHandler(InputPosition location, FunctionName name, List<Pattern> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}
	
	@Override
	public void clean() {
		logger.info("Cleaning " + name);
		RepositoryEntry.super.clean();
		this.hsiTree = null;
		this.type = null;
		this.convertedIntros = null;
		this.nestedVars = null;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public FunctionName name() {
		return name;
	}

	public boolean isMyName(NameOfThing other) {
		if (other == this.name)
			return true;
		return false;
	}

	@Override
	public boolean isObjAccessor() {
		return false;
	}
	
	@Override
	public int compareTo(LogicHolder o) {
		return name().compareTo(o.name());
	}

	@Override
	public boolean hasArgs() {
		return !args.isEmpty();
	}
	
	public List<Pattern> args() {
		return args;
	}

	public int argCount() {
		int ret = args.size();
		if (reportHolder && hasState())
			ret++;
		if (nestedVars != null)
			ret += nestedVars.size();
		return ret;
	}

	public int argCountWithoutHolder() {
		int ret = args.size();
		if (nestedVars != null)
			ret += nestedVars.size();
		return ret;
	}
	
	@Override
	public void setConstness(FunctionConstness fc) {
		this.constNess = fc;
	}
	
	@Override
	public FunctionConstness constNess() {
		return constNess;
	}

	@Override
	public void guard(GuardedMessages gm) {
		guards.add(gm);
	}

	@Override
	public boolean hasMessages() {
		if (super.hasMessages())
			return true;
		for (GuardedMessages gm : guards)
			if (gm.hasMessages())
				return true;
		return false;
	}
	
	public void nestedVars(NestedVarReader nestedVars) {
		this.nestedVars = nestedVars;
	}

	public NestedVarReader nestedVars() {
		return nestedVars;
	}

	public void bindHsi(HSITree hsiTree) {
		this.hsiTree = hsiTree;
	}

	public HSITree hsiTree() {
		return hsiTree;
	}

	@Override
	public HSICases hsiCases() {
		if (convertedIntros == null)
			throw new RuntimeException("Method has not been converted");
		return new FunctionHSICases(convertedIntros);
	}
	
	public List<Slot> slots() {
		List<Slot> slots = new ArrayList<>();
		for (int i=0;i<hsiTree.width();i++) {
			slots.add(new ArgSlot(i, hsiTree.get(i)));
		}
		return slots;
	}

	public void conversion(List<FunctionIntro> convertedIntros) {
		this.convertedIntros = convertedIntros;
	}

	public boolean isConverted() {
		return convertedIntros != null;
	}
	
	public List<FunctionIntro> converted() {
		if (convertedIntros == null)
			throw new NotImplementedException("there is no converted function");
		return convertedIntros;
	}

	public void bindType(Type ty) {
		if (this.type != null)
			throw new RuntimeException("Cannot bind type of " + this.name + " more than once");
		this.type = ty;
	}

	public boolean hasType() {
		return this.type != null;
	}
	
	public Type type() {
		if (this.dynamicType) {
			throw new CantHappenException("To resolve dynamic types, use resolveType");
		}
		if (this.type == null)
			throw new RuntimeException("Type not bound for " + name.uniqueName());
		return this.type;
	}

	public void dynamicallyType() {
		this.dynamicType  = true;
	}

	public Type resolveType(RepositoryReader repository) {
		if (!dynamicType) {
			return type();
		}
		if (this.args.isEmpty()) {
			return this.getObject();
		} else {
			ArrayList<Type> types = new ArrayList<>();
			for (Pattern p : this.args) {
				if (p instanceof TypedPattern) {
					TypedPattern tp  = (TypedPattern) p;
					types.add(tp.type.resolveType(repository));
				} else
					throw new NotImplementedException("not a typed pattern");
				
			}
			types.add(this.getObject());
			return new Apply(types);
		}
	}
	
	public void reportHolderInArgCount() {
		reportHolder = true;
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("ObjectCtor[" + toString() + "]");
	}

	public abstract boolean hasState();
	public abstract boolean hasObject();
	public abstract boolean hasImplements();
	public abstract boolean isEvent();
	public abstract ObjectDefn getObject();
	public abstract Implements getImplements();
	public abstract EventHolder getCard();
	public abstract ContractMethodDecl contractMethod();
}
