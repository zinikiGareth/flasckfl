package test.tokenizers;

import static org.junit.Assert.*;

import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.tokenizers.Tokenizable;
import org.junit.Test;

public class TokenizationTests {

	@Test
	public void findCanFindAStringInTheBuffer() {
		Tokenizable tkz = line("hello -> world");
		assertEquals(6, tkz.find("->"));
		assertEquals(0, tkz.at());
	}

	@Test
	public void findOnlyLooksForward() {
		Tokenizable tkz = line("hello -> world");
		tkz.reset(7);
		assertEquals(-1, tkz.find("->"));
		assertEquals(7, tkz.at());
	}

	@Test
	public void althoughForwardStartsNow() {
		Tokenizable tkz = line("hello -> world");
		tkz.reset(6);
		assertEquals(6, tkz.find("->"));
		assertEquals(6, tkz.at());
	}

	@Test
	public void findDoesNotFindAStringInTheBufferAfterSlashSlash() {
		Tokenizable tkz = line("hello // -> world");
		assertEquals(-1, tkz.find("->"));
		assertEquals(0, tkz.at());
	}

	@Test
	public void findDoesNotMatchInsideStrings() {
		Tokenizable tkz = line("hello '->' world");
		assertEquals(-1, tkz.find("->"));
		assertEquals(0, tkz.at());
	}

	@Test
	public void slashSlashInAStringDoesNotCount() {
		Tokenizable tkz = line("hello '//' -> world");
		assertEquals(11, tkz.find("->"));
		assertEquals(0, tkz.at());
	}

	@Test
	public void anUnterminatedStringIsIgnored() {
		Tokenizable tkz = line("hello '// -> world");
		assertEquals(-1, tkz.find("->"));
		assertEquals(0, tkz.at());
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TestSupport.line(string));
	}

}
