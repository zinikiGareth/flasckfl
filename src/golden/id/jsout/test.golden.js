if (typeof test === 'undefined') {
  test = function() {
  }
}

if (typeof test.golden === 'undefined') {
  test.golden = function() {
  }
}

test.golden.id = function(v0) {
  "use strict";
  return v0;
}

test.golden;
