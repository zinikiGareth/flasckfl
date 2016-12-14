package investigations.ui4j;

import static org.junit.Assert.*;

import org.junit.Test;
import org.zinutils.sync.SyncUtils;

import com.ui4j.api.browser.BrowserEngine;
import com.ui4j.api.browser.BrowserFactory;
import com.ui4j.api.browser.Page;
import com.ui4j.api.dom.Element;
import com.ui4j.test.JSObjectTest;

import netscape.javascript.JSObject;

public class InteractWithUI4J {

	@Test
	public void testCanWeInsertMinimalHTMLIntoTheWebKitBrowserLikeItSaysInTheDocco() throws Exception {
        // get the instance of the webkit
        BrowserEngine browser = BrowserFactory.getWebKit();

        // navigate to blank page
        Page page = browser.navigate("about:blank");

        // show the browser page
        page.show();

        // append html header to the document body
        page.getDocument().getBody().append("<h1>Hello, World!</h1>");

        page.getDocument().queryAll("h1").forEach(e -> {
			assertEquals("Hello, World!", e.getText().get());
		});

	}
	
	@Test
	public void testWeCanReadFromHackerNewsAsIsSaysInTheDocco() throws Exception {
		Page page = BrowserFactory.getWebKit().navigate("https://news.ycombinator.com");
		page.getDocument().queryAll(".title a").forEach(e -> {
			System.out.println(e.getText().get());
		});
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
}
