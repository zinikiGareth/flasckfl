package org.flasck.flas.parsedForm.st;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.st.SystemTestDefinitionConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.zinutils.exceptions.NotImplementedException;

public class SystemTest implements SystemTestDefinitionConsumer, RepositoryEntry {
	private final UnitTestFileName stfn;
	public SystemTestConfiguration configure;
	public final List<SystemTestStage> stages = new ArrayList<>();
	public SystemTestCleanup cleanup;

	public SystemTest(UnitTestFileName stfn) {
		this.stfn = stfn;
	}

	@Override
	public void configure(SystemTestConfiguration utc) {
		this.configure = utc;
	}

	@Override
	public void test(SystemTestStage stage) {
		stages.add(stage);
	}

	@Override
	public void cleanup(SystemTestCleanup utc) {
		this.cleanup = utc;
	}

	public NameOfThing name() {
		return stfn;
	}

	@Override
	public InputPosition location() {
		throw new NotImplementedException();
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println("SystemTest[" + stfn.uniqueName() + "]");
	}

	@Override
	public void newHandler(ErrorReporter errors, HandlerImplements hi) {
	}

	@Override
	public void newIntroduction(ErrorReporter errors, IntroduceVar var) {
	}

	@Override
	public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName leadName, FunctionName pkgName,
			Expr expr) {
	}

	@Override
	public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
	}

	@Override
	public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
	}

	@Override
	public void argument(ErrorReporter errors, VarPattern parm) {
	}

	@Override
	public void argument(ErrorReporter errors, TypedPattern with) {
	}

	@Override
	public void polytype(ErrorReporter errors, PolyType pt) {
	}

	@Override
	public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
	}
	

	@Override
	public String toString() {
		return "SystemTest[" + stfn.uniqueName() + "]";
	}
}
