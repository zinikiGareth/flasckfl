package test.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TCStateTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final FunctionName nameF = FunctionName.function(pos, pkg, "f");
	FunctionDefinition fnF = new FunctionDefinition(nameF, 1);
	final FunctionName nameG = FunctionName.function(pos, pkg, "g");
	FunctionDefinition fnG = new FunctionDefinition(nameG, 1);
	List<Pattern> args = new ArrayList<>();
	FunctionIntro fiF1 = new FunctionIntro(nameF, args);
	FunctionIntro fiF2 = new FunctionIntro(nameF, args);
	FunctionIntro fiG1 = new FunctionIntro(nameG, args);
	FunctionIntro fiG2 = new FunctionIntro(nameG, args);
	private FunctionGroup grp = new DependencyGroup(fnF, fnG);

	@Before
	public void begin() {
		context.checking(new Expectations() {{
			fnF.intro(fiF1);
			fnF.intro(fiF2);
			fnG.intro(fiG1);
			fnG.intro(fiG2);
		}});
	}

	@Test
	public void aNewStateInitializesItsGroupMembersAsUTs() {
		CurrentTCState state = new FunctionGroupTCState(repository, grp);
		state.requireVarConstraints(pos, nameF.uniqueName());
		state.requireVarConstraints(pos, nameG.uniqueName());
	}

}
