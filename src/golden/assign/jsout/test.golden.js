if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Container = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Container';
  if (v0) {
    if (v0.value) {
      this.value = v0.value;
    }
  }
  else {
  }
}

test.golden.Container = function(v0) {
  "use strict";
  return new test.golden._Container({value: v0});
}

test.golden._Card = function(v0) {
  "use strict";
  var _self = this;
  this._ctor = 'test.golden.Card';
  this._wrapper = v0.wrapper;
  this._special = 'card';
  this._services = {};
  this._contracts = {};
}

test.golden._Card.prototype._onReady = function(v0) {
  "use strict";
  var msgs = {curr: Nil};
  return msgs.curr;
}

test.golden.Card = function(v0) {
  "use strict";
  return new test.golden._Card(v0);
}

test.golden._Card.prototype._render = function(doc, wrapper, parent) {
  "use strict";
}

test.golden.Card.prototype.doit = function() {
  "use strict";
  var v0 = FLEval.closure(FLEval.field, this, 'c');
  var v1 = FLEval.closure(Assign, v0, 'value', 'hello');
  return FLEval.closure(Cons, v1, Nil);
}

test.golden;
