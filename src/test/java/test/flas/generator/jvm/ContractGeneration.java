package test.flas.generator.jvm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.JVMGenerator;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.JavaInfo.Access;

public class ContractGeneration {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void simpleContractDeclarationForcesThreeClassesToBeGenerated() {
		ByteCodeStorage bce = context.mock(ByteCodeStorage.class);
		ByteCodeSink parent = context.mock(ByteCodeSink.class, "parent");
		ByteCodeSink up = context.mock(ByteCodeSink.class, "up");
		ByteCodeSink down = context.mock(ByteCodeSink.class, "down");
		String pname = "test.repo.MyContract";
		context.checking(new Expectations() {{
			oneOf(bce).newClass(pname); will(returnValue(parent));
			allowing(parent).getCreatedName(); will(returnValue(pname));

			oneOf(bce).newClass("test.repo.MyContract$Up"); will(returnValue(up));
			allowing(up).generateAssociatedSourceFile();

			oneOf(bce).newClass("test.repo.MyContract$Down"); will(returnValue(down));
			
			oneOf(parent).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Up");
			oneOf(up).makeInterface();
			oneOf(up).addInnerClassReference(Access.PUBLICSTATICINTERFACE, pname, "Up");
			oneOf(up).implementsInterface("org.ziniki.ziwsh.UpContract");
			
		}});
		JVMGenerator gen = new JVMGenerator(bce);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractDecl cd = new ContractDecl(pos, pos, cname);
		new Traverser(gen).visitContractDecl(cd);
	}
}
