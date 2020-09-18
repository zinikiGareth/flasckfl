A fairly minimal setup has a configuration, stage and cleanup

	configure
		data Minimal m
		
	test that m was initialized
		assert (m.value)
			42
		
	finally
		// we could check that all expectations have been met