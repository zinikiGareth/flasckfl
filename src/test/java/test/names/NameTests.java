package test.names;

import static org.junit.Assert.*;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.junit.Test;

public class NameTests {
	@Test
	public void testSimpleSolidNameBecomesFullName() {
		assertEquals("demo.ziniki.Account", new SolidName(new PackageName("demo.ziniki"), "Account").javaClassName());
	}
}
