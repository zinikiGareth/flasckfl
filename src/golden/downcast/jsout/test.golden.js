if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Role = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Role';
  if (v0) {
    if (v0.r) {
      this.r = v0.r;
    }
  }
  else {
  }
}

test.golden.Role = function(v0) {
  "use strict";
  return new test.golden._Role({r: v0});
}

test.golden.f = function(v0) {
  "use strict";
  var v1 = FLEval.closure(v0);
  return FLEval.closure(FLEval.field, v1, 'r');
}

test.golden;
