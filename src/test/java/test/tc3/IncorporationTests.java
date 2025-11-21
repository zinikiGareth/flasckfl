package test.tc3;

import static org.junit.Assert.*;

import java.net.URI;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.junit.Test;

public class IncorporationTests {
	private URI fred = URI.create("file:/fred");
	private InputPosition pos = new InputPosition(fred, 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void contractsIncorporateThemselves() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "CD"));
		assertTrue(cd.incorporates(pos, cd));
	}

}
