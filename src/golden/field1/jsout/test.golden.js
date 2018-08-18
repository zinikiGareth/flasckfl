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

test.golden.Card.prototype.doit = function(v0, v1) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (FLEval.isA(v0, 'test.golden.Container')) {
    var v2 = FLEval.closure(FLEval.field, this, 'c');
    var v3 = FLEval.closure(FLEval.field, v0, 'value');
    var v4 = FLEval.closure(Assign, v2, 'value', v3);
    return FLEval.closure(Cons, v4, Nil);
  }
  return FLEval.error("test.golden.Card.doit: case not handled");
}

test.golden;
