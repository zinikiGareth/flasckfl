package org.flasck.flas.parsedForm;

public enum CodeType {
	// standalone, package-scoped function
	FUNCTION {
	},
	// card-scoped function (method)
	CARD {
	},
	// method on a contract declaration
	DECL {
	},
	// method on a contract impl
	CONTRACT {
	},
	// method on a service impl
	SERVICE {
	},
	// TODO: as with "HANDLERFUNCTION" below, it would seem possible to define a function in a nested scope of CONTRACT/SERVICE that needs special handling (i.e. it can access card members, but through "_card")
	// method on a handler impl
	HANDLER {
	},
	// function nested within a HANDLER method
	HANDLERFUNCTION {
	},
	// an event handler on a card
	EVENTHANDLER {
	},
	// a "class" connecting an element to an event handler
	// This really is a very different beast ... has no real HSIE code
	// Can we extract it?
	EVENT {
	},
	// a standalone method
	STANDALONE {
	},
	// a method on an area
	AREA {
	},
	OBJECT {
	},
	OCTOR {
	},
	INITIALIZER {
	};

	public boolean isHandler() {
		return this == CONTRACT || this == SERVICE || this == HANDLER || this == AREA;
	}
	
	public boolean hasThis() { // may well be more
		return this == OBJECT;
	}
}