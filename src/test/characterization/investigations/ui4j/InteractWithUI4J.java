package investigations.ui4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;

import netscape.javascript.JSObject;

public class InteractWithUI4J {

	@Test
	public void testCanWeInsertMinimalHTMLIntoTheWebKitBrowserLikeItSaysInTheDocco() throws Exception {
        // get the instance of the webkit
        BrowserEngine browser = BrowserFactory.getWebKit();

        // navigate to blank page
        Page page = browser.navigate("about:blank");

        // show the browser page
//        page.show();

        // append html header to the document body
        page.getDocument().getBody().append("<h1>Hello, World!</h1>");

        page.getDocument().queryAll("h1").forEach(e -> {
			assertEquals("Hello, World!", e.getText().get());
		});

	}
	
	@Test
	public void testWeCanReadFromHackerNewsAsIsSaysInTheDocco() throws Exception {
		Page page = BrowserFactory.getWebKit().navigate("https://news.ycombinator.com");
		AtomicInteger count = new AtomicInteger();
		page.getDocument().queryAll(".title a").forEach(e -> {
			count.incrementAndGet();
		});
		System.out.println("count = " + count);
	}
	
	@Test
	public void testWeCanLoadAFileWithNestedFLASJavaScript() throws Exception {
		BrowserEngine webKit = BrowserFactory.getWebKit();
		assertNotNull(webKit);
		Page page = webKit.navigate(getClass().getResource("/jsrunner/test1/test1.html").toExternalForm());
		assertNotNull(page);
        JSObject object = (JSObject) page.executeScript("window.test.hello.Hello");
        assertNotNull(object);
	}
	
	@Test
	public void testWeCanCreateANewCard() throws Exception {
		BrowserEngine webKit = BrowserFactory.getWebKit();
		Page page = webKit.navigate(getClass().getResource("/jsrunner/test1/test1.html").toExternalForm());
        Object object = page.executeScript("window.test.hello.Hello(body)");
        assertNotNull(object);
        System.out.println("object = " + object);
	}
	
	@Test
	public void testWeCanQueryTheCurrentElementsAddedProgrammatically() throws Exception {
		BrowserEngine webKit = BrowserFactory.getWebKit();
		Page page = webKit.navigate("about:blank");
		Element body = page.getDocument().getBody();
        body.append("<div><div id='x'>hello</div><div id='y'>there</div></div>");
        System.out.println(body);
        List<Element> divX = page.getDocument().queryAll("#x");
        System.out.println(divX);
        assertEquals(1, divX.size());
        Element e = divX.get(0);
        assertEquals("div", e.getTagName());
        assertEquals("x", e.getId().get());
        assertEquals("<div id=\"x\">hello</div>", e.getOuterHTML());
        assertEquals("hello", e.getInnerHTML());

        assertEquals(0, page.getDocument().queryAll("div.show").size());
	}
}
