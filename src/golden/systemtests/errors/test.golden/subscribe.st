	configure
		ajax create bert "https://baseuri"
			subscribe
		ajax create fred "https://baseuri"
			subscribe "g g"
		ajax create jack "https://baseuri"
			subscribe "https://foo"
		ajax create jill "https://baseuri"
			subscribe "foo"
		ajax create megan "https://baseuri"
			subscribe ""
		