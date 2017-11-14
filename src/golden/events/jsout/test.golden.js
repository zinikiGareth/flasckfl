if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Events = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Events';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden.Events = function(v0) {
  "use strict";
  return new test.golden._Events(v0);
}

test.golden._Events.prototype._render = function(doc, wrapper, parent) {
  "use strict";
  new test.golden._Events.B1(new CardArea(parent, wrapper, this));
}

test.golden._Events.B1 = function(parent) {
  DivArea.call(this, parent);
  if (!parent) return;
  var b2 = new test.golden._Events.B2(this);
  var b3 = new test.golden._Events.B3(this);
}

test.golden._Events.B1.prototype = new DivArea();

test.golden._Events.B1.prototype.constructor = test.golden._Events.B1;

test.golden._Events.B2 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('OK');
  this._mydiv.className = 'button';
  test.golden._Events.B2.prototype._add_handlers.call(this);
}

test.golden._Events.B2.prototype = new TextArea();

test.golden._Events.B2.prototype.constructor = test.golden._Events.B2;

test.golden._Events.B2.prototype._add_handlers = function() {
  this._mydiv['onclick'] = function(event) {
    this._area._wrapper.dispatchEvent(this._area.handlers_0(), event);
  }
}

test.golden._Events.B3 = function(parent) {
  TextArea.call(this, parent);
  if (!parent) return;
  this._setText('Set13');
  this._mydiv.className = 'button';
  test.golden._Events.B3.prototype._add_handlers.call(this);
}

test.golden._Events.B3.prototype = new TextArea();

test.golden._Events.B3.prototype.constructor = test.golden._Events.B3;

test.golden._Events.B3.prototype._add_handlers = function() {
  this._mydiv['onclick'] = function(event) {
    this._area._wrapper.dispatchEvent(this._area.handlers_1(), event);
  }
}

test.golden.Events.handlers_0.prototype.getHandler = function() {
  "use strict";
  return test.golden.Events.prototype.okThen;
}

test.golden.Events.handlers_1.prototype.getHandler = function() {
  "use strict";
  return FLEval.oclosure(this._card, FLEval.curry, test.golden.Events.prototype.setTo, 2, 13);
}

test.golden.Events.prototype.okThen = function(v0) {
  "use strict";
  var v1 = FLEval.closure(Debug, v0);
  return FLEval.closure(Cons, v1, Nil);
}

test.golden.Events.prototype.setTo = function(v0, v1) {
  "use strict";
  var v2 = FLEval.closure(Debug, v1);
  return FLEval.closure(Cons, v2, Nil);
}

test.golden;
