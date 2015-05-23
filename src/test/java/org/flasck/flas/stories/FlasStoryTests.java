package org.flasck.flas.stories;

import static org.junit.Assert.*;

import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.sampleData.BlockTestData;
import org.junit.Test;

public class FlasStoryTests {

	@Test
	public void test() {
		Object o = new FLASStory().process("MathLib", BlockTestData.allFib());
		assertNotNull(o);
		assertTrue(o instanceof Scope);
		Scope s = (Scope) o;
		assertEquals(1, s.size());
		HSIE.applyTo(s);
	}

}
