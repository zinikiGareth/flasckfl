#FLASCK - Functional Compiler for Cards#

This is a compiler for a state/transformation based functional language for cards.  One day, there will be a full set of documentation here, but for now the best that can be offered is the set of notes that have been collected along the way at the [GitHub Documentation Site][1].

##Basic Philosophy##

This work emerges from the work that Chris Tse's team have done on cards in JavaScript with advanced frameworks at [cardstack.io][2].  The main distinction is that instead of trying to build cards with a combination of JavaScript, a JavaScript framework and a card library, this language (and it's [companion runtime][3] with [GitHub experimental notes][4]) attempts to provide a view of cards that is simple and consistent.

Each card is defined as a combination of _state_, _template_ and _code_.

The **state** defines data that can be stored on the card and is essentially transactional and based on a "clock" or "gating" mechanism, much like JavaScript, where the values move between states at the end of each execution cycle.

The **template** defines HTML-like constructs that are based solely on the values in the state and are defined declaratively.  The runtime figures out the correct delta at the end of each execution cycle between the rendering of the old state and the rendering of the new state and makes the appropriate updates to the display.

The **code** portion consists of all the logic to handle state changes.  Mainly this consists of supporting _contracts_ in which the containing environment can provide data to, or make requests of, the card; and handling _events_ in the JavaScript sense.  These methods ultimately cause actions to take place, either updating the card state or making requests of the containing environment.

##Compilation Targets##

The main compilation target is JavaScript with the JavaScript runtime in [flasjs][3].

Currently, there is code which amounts to a proto-backend to compile directly to Java bytecodes to support native Android development.

There are plans to support generation of Swift code to generate native code for iOS.

[1]: http://zinikigareth.github.io/flasckfl/
[2]: http://cardstack.io/#architecture
[3]: https://github.com/zinikiGareth/flasjs
[4]: http://zinikigareth.github.io/flasjs/
