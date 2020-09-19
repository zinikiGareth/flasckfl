A fairly minimal setup has a configuration, stage and cleanup

	configure
		data Multiplier m
		
	test that m was initialized
		assert (m.value)
			42
	
	test we can multiply by 10
		contract m HitMe hit
		assert (m.value)
			420
			
	test we can multiply again because we run stages in order
		contract m HitMe hit
		assert (m.value)
			4200
	