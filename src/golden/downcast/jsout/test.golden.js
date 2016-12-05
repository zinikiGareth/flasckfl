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
    if (v0.id) {
      this.id = v0.id;
    }
  }
  else {
  }
}

test.golden.Role = function(v0) {
  "use strict";
  return new test.golden._Role({id: v0});
}

test.golden.f = function(v0) {
  "use strict";
  v0 = FLEval.head(v0);
  if (v0 instanceof FLError) {
    return v0;
  }
  if (v0) {
    var v1 = FLEval.closure(v0);
    return FLEval.closure(FLEval.field, v1, 'id');
  }
  return FLEval.error("test.golden.f: case not handled");
}

test.golden;
