/*
 * Copyright 2016 Memories, Dreams, and Reflections LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.memoriesdreamsandreflections.nester;


import java.io.BufferedWriter ;
import java.io.File ;
import java.io.FileWriter ;
import java.util.ArrayList ;
import java.util.LinkedHashMap ;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.fasterxml.jackson.core.JsonProcessingException ;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings( "rawtypes" )
public class NesterNameValueTest
{

	static Logger log = LoggerFactory.getLogger( NesterNameValueTest.class );
	
	List<TestNameValueStruct> testListNoNulls;
	static String[] states = {"ME", "NH", "VT", "MA", "CT", "RI", "NY", "NJ", "PA", "DE", "MD", "VA",
					"WV", "NC", "SC", "GA", "FL", "KY", "TN"};
	static String[] cities = {"CITY01", "CITY02", "CITY03", "CITY04", "CITY05", 
					"CITY06", "CITY07", "CITY08", "CITY09", "CITY10"};
	static String[] zips = {"00001", "00002", "00003", "00004", "00005",
					"00006", "00007", "00008", "00009", "00010"};
	static String[] secs = {"SEC-001", "SEC-002", "SEC-003", "SEC-004", "SEC-005", 
				"SEC-006", "SEC-007", "SEC-008", "SEC-009", "SEC-010"};
	
	static List<String> categories = Stream.of( "state", "city", "zip" ).collect( Collectors.toList() );
	static List<CategoryDescriptor<TestNameValueStruct>> categoryDescriptors = new ArrayList<>(4);

	@Before
	public void before()
	{
		testListNoNulls = new ArrayList<>(states.length * cities.length * zips.length * secs.length);
		for(String state : states)
		{
			for (String city : cities)
			{
				for(String zip : zips)
				{
					for (String sec : secs)
					{
						testListNoNulls.add( new TestNameValueStruct(state, 
							city,
							zip,
							sec) );
					}
				}
			}
		}
	}
	
	@Test
	public void mungeTest()
	throws Exception
	{
		List<CategoryDescriptor<TestNameValueStruct>> categoryDescriptors = new ArrayList<>(4);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "state" )
				.withAncillaryFieldNames( new String[]{"ancState"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "stateUrl", "http://ohcrap.com?state="  + row.getState());
					return ancMap;
				} )
				.withCollectionLabel("states")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "city" )
				.withAncillaryFieldNames( new String[]{"ancState", "ancCity"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "cityUrl", "http://ohcrap.com?state="  + row.getState() +
											"&city=" + row.getCity());
					return ancMap;
				} )
				.withCollectionLabel("cities")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "zip" )
				.withAncillaryValueMappingFunction( (row) -> {
						Map<String, Object> ancMap = new LinkedHashMap<>();
						ancMap.put( "zipAnc1", row.getZip() + " anc1" );
						ancMap.put( "zipAnc2", row.getZip() + " anc2" );
						ancMap.put( "zipUrl", "http://ohcrap.com?state="  + row.getState() +
												"&city=" + row.getCity() + 
												"&zip=" + row.getZip() );
						return ancMap;
					} )
				.withCollectionLabel("zips")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "security" )
				.withCollectionLabel("securities")
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "secAnc1", row.getSecurity() + " anc1" );
					ancMap.put( "secAnc2", row.getSecurity() + " anc2" );
					return ancMap;
				} )
				.withDetailCollectionLabel( "synthesizedDetails" )
				.withDetailValueMappingFunction( (row) -> {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put( "detailState", row.getState() );
					result.put( "detailCity", row.getCity() );
					result.put( "detailZip", row.getZip() );
					result.put( "detailSec", row.getSecurity() );
					return result;
				}
				)
			);

		Nester<TestNameValueStruct> nester = Nester.newInstance( TestNameValueStruct.class )
			.categoryDescriptors( categoryDescriptors );
		Map result = nester.nameValueNest( testListNoNulls );
		dumpToJsonFile(result, "mungeTestResult.json");
		
		Object queryResult = nester.query();
		Assert.assertTrue(queryResult instanceof Map && ((Map)queryResult).containsKey( "states" ));
		dumpToJsonFile(queryResult, "mungeTestQueryEntireTree.json");
		
		queryResult = nester.query("NY");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "cities" ));
		Assert.assertTrue(((Map)queryResult).get( "state" ).equals("NY"));
		dumpToJsonFile(queryResult, "mungeTestQueryState.json");
		
		queryResult = nester.query("NY", "CITY01");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "zips" ));
		Assert.assertTrue(((Map)queryResult).get( "city" ).equals("CITY01"));
		dumpToJsonFile(queryResult, "mungeTestQueryCity.json");
		
		queryResult = nester.query("NY", "CITY01", "00001");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "securities" ));
		Assert.assertTrue(((Map)queryResult).get( "zip" ).equals("00001"));
		dumpToJsonFile(queryResult, "mungeTestQueryZip.json");
		
		queryResult = nester.query("NY", "CITY01", "00001", "SEC-001");
		Assert.assertTrue(queryResult instanceof Map);
		Assert.assertTrue(((Map)queryResult).containsKey( "synthesizedDetails" ));
		Assert.assertTrue(((Map)queryResult).get( "security" ).equals("SEC-001"));
		dumpToJsonFile(queryResult, "mungeTestQuerySecurity.json");
		
	}
	
	
	@Test
	public void mungeTestWithSubstitutedNames()
	throws Exception
	{
		List<CategoryDescriptor<TestNameValueStruct>> categoryDescriptors = new ArrayList<>(4);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "state" )
				.withCategoryFieldRenderedName( "rendered_state")
				.withAncillaryFieldNames( new String[]{"ancState"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "stateUrl", "http://ohcrap.com?state="  + row.getState());
					return ancMap;
				} )
				.withCollectionLabel("states")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "city" )
				.withCategoryFieldRenderedName( "rendered_city" )
				.withAncillaryFieldNames( new String[]{"ancState", "ancCity"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "cityUrl", "http://ohcrap.com?state="  + row.getState() +
											"&city=" + row.getCity());
					return ancMap;
				} )
				.withCollectionLabel("cities")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "zip" )
				.withCategoryFieldRenderedName( "rendered_zip" )
				.withAncillaryValueMappingFunction( (row) -> {
						Map<String, Object> ancMap = new LinkedHashMap<>();
						ancMap.put( "zipAnc1", row.getZip() + " anc1" );
						ancMap.put( "zipAnc2", row.getZip() + " anc2" );
						ancMap.put( "zipUrl", "http://ohcrap.com?state="  + row.getState() +
												"&city=" + row.getCity() + 
												"&zip=" + row.getZip() );
						return ancMap;
					} )
				.withCollectionLabel("zips")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "security" )
				.withCategoryFieldRenderedName( "rendered_security" )
				.withCollectionLabel("securities")
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "secAnc1", row.getSecurity() + " anc1" );
					ancMap.put( "secAnc2", row.getSecurity() + " anc2" );
					return ancMap;
				} )
				.withDetailCollectionLabel( "synthesizedDetails" )
				.withDetailValueMappingFunction( (row) -> {
					Map<String, Object> result = new LinkedHashMap<>();
					result.put( "detailState", row.getState() );
					result.put( "detailCity", row.getCity() );
					result.put( "detailZip", row.getZip() );
					result.put( "detailSec", row.getSecurity() );
					return result;
				}
				)
			);

		String renderedState = categoryDescriptors.get( 0 ).renderedCategoryFieldName();
		String renderedCity = categoryDescriptors.get( 1 ).renderedCategoryFieldName();
		String renderedZip = categoryDescriptors.get( 2 ).renderedCategoryFieldName();
		String renderedSecurity = categoryDescriptors.get( 3 ).renderedCategoryFieldName();
		
		Nester<TestNameValueStruct> nester = Nester.newInstance( TestNameValueStruct.class )
			.categoryDescriptors( categoryDescriptors );
		Map result = nester.nameValueNest( testListNoNulls );
		dumpToJsonFile(result, "mungeTestWithSubstitutedNamesResult.json");
		
		Object queryResult = nester.query();
		Assert.assertTrue(queryResult instanceof Map && ((Map)queryResult).containsKey( "states" ));
		dumpToJsonFile(queryResult, "mungeTestWithSubstitutedNamesQueryEntireTree.json");
		
		queryResult = nester.query("NY");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "cities" ));
		Assert.assertTrue(((Map)queryResult).get( renderedState ).equals("NY"));
		dumpToJsonFile(queryResult, "mungeTestWithSubstitutedNamesQueryState.json");
		
		queryResult = nester.query("NY", "CITY01");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "zips" ));
		Assert.assertTrue(((Map)queryResult).get( renderedCity ).equals("CITY01"));
		dumpToJsonFile(queryResult, "mungeTestWithSubstitutedNamesQueryCity.json");
		
		queryResult = nester.query("NY", "CITY01", "00001");
		Assert.assertTrue(queryResult instanceof Map); 
		Assert.assertTrue(((Map)queryResult).containsKey( "securities" ));
		Assert.assertTrue(((Map)queryResult).get( renderedZip ).equals("00001"));
		dumpToJsonFile(queryResult, "mungeTestWithSubstitutedNamesQueryZip.json");
		
		queryResult = nester.query("NY", "CITY01", "00001", "SEC-001");
		Assert.assertTrue(queryResult instanceof Map);
		Assert.assertTrue(((Map)queryResult).containsKey( "synthesizedDetails" ));
		Assert.assertTrue(((Map)queryResult).get( renderedSecurity ).equals("SEC-001"));
		dumpToJsonFile(queryResult, "mungeTestWithSubstitutedNamesQuerySecurity.json");
		
	}

	@Test
	public void mungeTestNoDetails()
	throws Exception
	{
		List<CategoryDescriptor<TestNameValueStruct>> categoryDescriptors = new ArrayList<>(4);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "state" )
				.withAncillaryFieldNames( new String[]{"ancState"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "stateUrl", "http://ohcrap.com?state="  + row.getState());
					return ancMap;
				} )
				.withCollectionLabel("states")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "city" )
				.withAncillaryFieldNames( new String[]{"ancState", "ancCity"} )
				.withAncillaryValueMappingFunction( (row) -> {
					Map<String, Object> ancMap = new LinkedHashMap<>();
					ancMap.put( "cityUrl", "http://ohcrap.com?state="  + row.getState() +
											"&city=" + row.getCity());
					return ancMap;
				} )
				.withCollectionLabel("cities")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "zip" )
				.withAncillaryValueMappingFunction( (row) -> {
						Map<String, Object> ancMap = new LinkedHashMap<>();
						ancMap.put( "zipAnc1", row.getZip() + " anc1" );
						ancMap.put( "zipAnc2", row.getZip() + " anc2" );
						ancMap.put( "zipUrl", "http://ohcrap.com?state="  + row.getState() +
												"&city=" + row.getCity() + 
												"&zip=" + row.getZip() );
						return ancMap;
					} )
				.withCollectionLabel("zips")
			);
			categoryDescriptors.add( CategoryDescriptor.newInstance( TestNameValueStruct.class )
				.withCategoryFieldName( "security" )
				.withCollectionLabel("securities")
			);

		Nester<TestNameValueStruct> nester = Nester.newInstance( TestNameValueStruct.class )
			.categoryDescriptors( categoryDescriptors );
		Map result = nester.nameValueNest( testListNoNulls );
		dumpToJsonFile(result, "mungeTestResult.json");
		
		Object queryResult = nester.query();
		dumpToJsonFile(queryResult, "mungeTestNoDetailsQueryEntireTree.json");
		
		queryResult = nester.query("NY", "CITY01", "00001", "SEC-001");
		Assert.assertTrue(queryResult instanceof String);
		Assert.assertTrue(((String)queryResult).equals("SEC-001"));
		dumpToJsonFile(queryResult, "mungeTestNoDetailsQuerySecurity.json");
		
	}

	private void dumpToJsonFile(Object object, String fileName) throws Exception
	{
		ObjectMapper ser = new ObjectMapper();
		String json = ser.writeValueAsString( object );

		File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
		log.info( "Java_tmp_dir={}", tmpDir);
		File jsonDump = new File(tmpDir, fileName);
		BufferedWriter wrt = new BufferedWriter( new FileWriter(jsonDump));
		wrt.append( json );
		wrt.flush();
	}

}
