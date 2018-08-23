if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden._Entity = function(v0) {
  "use strict";
  this._ctor = 'test.golden.Entity';
  if (v0) {
    if (v0.id) {
      this.id = v0.id;
    }
  }
  else {
  }
}

test.golden.Entity = function() {
  "use strict";
  return new test.golden._Entity({});
}

test.golden._WithState = function() {
  "use strict";
}

test.golden;
