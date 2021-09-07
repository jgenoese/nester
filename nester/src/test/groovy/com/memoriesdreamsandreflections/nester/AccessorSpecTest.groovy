package com.memoriesdreamsandreflections.nester

import static org.junit.Assert.*

import org.junit.Test

import spock.lang.Specification

class AccessorSpecTest
extends Specification
{

	def testGetCategoryValues()
	{
		given:
		int a = 0
		int b = 1
		
		expect:
		 a == b
		
	}

	def testGetDetail()
	{
		given:
		int a = 0
		int b = 1
		
		expect:
		 a == b
	}

}
