package test.tc3;

import static org.junit.Assert.*;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.junit.Test;

public class IncorporationTests {
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void contractsIncorporateThemselves() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "CD"));
		assertTrue(cd.incorporates(pos, cd));
	}

}
