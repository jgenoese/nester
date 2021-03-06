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
import java.io.IOException ;
import java.util.ArrayList ;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger ;
import org.slf4j.LoggerFactory ;

import com.fasterxml.jackson.core.JsonGenerator ;
import com.fasterxml.jackson.databind.JsonSerializer ;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider ;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer ;
import com.fasterxml.jackson.databind.module.SimpleModule ;
import com.fasterxml.jackson.databind.ser.std.StdSerializer ;

@SuppressWarnings( "rawtypes" )
public class NesterTest
{

	static Logger log = LoggerFactory.getLogger( NesterTest.class );
	
	List<TestStruct> testListNoNulls;
	static String[] states = {null, "ME", "NH", "VT", "MA", "CT", "RI", "NY", "NJ", "PA", "DE", "MD", "VA",
					"WV", "NC", "SC", "GA", "FL", "KY", "TN"};
	static String[] cities = {null, "CITY01", "CITY02", "CITY03", "CITY04", "CITY05", 
					"CITY06", "CITY07", "CITY08", "CITY09", "CITY10"};
	static String[] zips = {null, "00001", "00002", "00003", "00004", "00005",
					"00006", "00007", "00008", "00009", "00010"};
	static String[] secs = {"SEC-001", "SEC-002", "SEC-003", "SEC-004", "SEC-005", 
				"SEC-006", "SEC-007", "SEC-008", "SEC-009", "SEC-010"};
	
	static List<String> categories = Stream.of( "state", "city", "zip" ).collect( Collectors.toList() );
	static List<CategoryDescriptor<TestStruct>> categoryDescriptors = new ArrayList<>(4);
	static 
	{
		categoryDescriptors.add( CategoryDescriptor.newInstance( TestStruct.class )
			.withCategoryFieldName( "state" ));
		categoryDescriptors.add( CategoryDescriptor.newInstance( TestStruct.class )
			.withCategoryFieldName( "city" ));
		categoryDescriptors.add( CategoryDescriptor.newInstance( TestStruct.class )
			.withCategoryFieldName( "zip" ));
	}

	@Before
	public void before()
	{
		testListNoNulls = new ArrayList<>(states.length * cities.length * zips.length * secs.length);
		lState: for(String state : states)
		{
			if (state == null)
			{
				testListNoNulls.add( new TestStruct(null, null, null, "GLOBAL_ROLLUP"));
				continue lState;
			}
			lCity: for (String city : cities)
			{
				if (city == null)
				{
					testListNoNulls.add( new TestStruct(state, null, null, state + "|STATE_ROLLUP"));
					continue lCity;
				}
				String cityLabel = state + "|" + city;
				lZip: for(String zip : zips)
				{
					if (zip == null)
					{
						testListNoNulls.add( new TestStruct(state, cityLabel, null, cityLabel + "|CITY_ROLLUP"));
						continue lZip;
					}
					for (String sec : secs)
					{
						testListNoNulls.add( new TestStruct(state, 
							cityLabel,
							zip,
							state + "|" + city + "|" + zip + "|" + sec) );
					}
				}
			}
		}
	}

	@Test
	public void nesterTestBuilderApi()
	throws Exception
	{
		TestStructAccessor accessor = new TestStructAccessor();
		TestStructAccessor accessorWithDetail = new TestStructAccessorWithDetail();
		
		
		Nester<TestStruct> nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail )
		.detailFieldName( "detail" );
		Assert.assertSame( categoryDescriptors, nester.categoryDescriptors );
		Assert.assertEquals( accessorWithDetail, nester.accessor );
		Assert.assertNull( nester.detailFieldName ); // accessor takes precedence
		
		Assert.assertTrue( nester.collectDetails );
		
		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.detailFieldName( "detail" );
		Assert.assertSame( categoryDescriptors, nester.categoryDescriptors );
		Assert.assertNotSame( accessor, nester.accessor );
		Assert.assertNotSame( accessorWithDetail, nester.accessor );
		Assert.assertEquals( "detail", nester.detailFieldName ); // accessor takes precedence
		
		Assert.assertTrue( nester.collectDetails );

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail )
		.collectDetails( false )
		.detailFieldName( "detail" );
		
		Assert.assertFalse( nester.collectDetails );
		
	}

	@Test
	public void nesterTest()
	throws Exception
	{
		TestStructAccessor accessor = new TestStructAccessor();
		TestStructAccessor accessorWithDetail = new TestStructAccessorWithDetail();
		File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
		log.info( "Java_tmp_dir={}", tmpDir);

		Nester<TestStruct> nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail )
		.detailFieldName( "detail" ); 
		Map result = nester.nest( testListNoNulls );
		log.info( "nesterTest Nester=[unordered,detailFieldName], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
		
		commonAssertions(result, TestStruct.Detail.class, new File(tmpDir, "subrow.json") );

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors );
		result = nester.nest( testListNoNulls );
		log.info( "nesterTest Nester=[unordered], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
 		
		commonAssertions(result, TestStruct.class, null);

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessor );
		result = nester.nest( testListNoNulls );
		log.info( "nesterTest Nester=[unordered, accessor], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
 		
		commonAssertions(result, TestStruct.class, new File(tmpDir, "fullrow.json"));

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail );
		result = nester.nest( testListNoNulls );
		log.info( "nesterTest Nester=[unordered, accessorWithDetail], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
 		
		commonAssertions(result, TestStruct.Detail.class, null);

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.detailFieldName( "detail" )
		.ordered();
		result = nester.nest( testListNoNulls );
		log.info( "nesterTest Nester=[ordered,detailFieldName], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
		
		commonAssertions(result, TestStruct.Detail.class, null);

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.ordered();
		result = nester.nest( testListNoNulls );
		log.info( "nesterTestPerformance01 Nester=[ordered], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );

		commonAssertions(result, TestStruct.class, null);

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.ordered()
		.accessor( accessor );
		result = nester.nest( testListNoNulls );
		log.info( "nesterTestPerformance01 Nester=[ordered, accessor], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
 		
		commonAssertions(result, TestStruct.class, null);

		nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.ordered()
		.accessor( accessorWithDetail );
		result = nester.nest( testListNoNulls );
		log.info( "nesterTestPerformance01 Nester=[ordered, accessorWithDetail], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
		
		commonAssertions(result, TestStruct.Detail.class, null);
	}
	
	@Test
	public void exploreNesterWithoutRollups()
	throws Exception
	{
		TestStructAccessor accessorWithDetail = new TestStructAccessorWithDetail();
		File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
		File jsonDump = new File(tmpDir, "exploreNester.json");
		log.info( "Java_tmp_dir={}", tmpDir);

		Nester<TestStruct> nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail )
		.detailFieldName( "detail" ); 
		Map result = nester.nest( testListNoNulls );

		dumpToJsonFile(result, jsonDump);
	}

	
	@Test
	public void exploreNesterWithoutDetails()
	throws Exception
	{
		TestStructAccessor accessorWithDetail = new TestStructAccessorWithDetail();
		File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
		File jsonDump = new File(tmpDir, "exploreNester.json");
		log.info( "Java_tmp_dir={}", tmpDir);

		Nester<TestStruct> nester = Nester.newInstance( TestStruct.class )
		.categoryDescriptors( categoryDescriptors )
		.accessor( accessorWithDetail )
		.collectDetails( false )
		.detailFieldName( "detail" ); 
		Map result = nester.nest( testListNoNulls );

		dumpToJsonFile(result, jsonDump);
	}
	
    @Test
    public void nesterTestShort()
    throws Exception
    {
        TestStructAccessorShort accessorShort = new TestStructAccessorShort();
        File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
        log.info( "Java_tmp_dir={}", tmpDir);

        Nester<TestStruct> nester = Nester.newInstance( TestStruct.class )
        .categoryDescriptors( categoryDescriptors.subList(0,1) )
        .accessor( accessorShort );

        Map result = nester.nest( testListNoNulls );
        log.info( "nesterTest Nester=[unordered,detailFieldName], execution_time={}, levelClocks={}", nester.getNanos(), nester.getLevelClocks() );
		 
        /*
         * This test can't use commonAssertions because it doesn't use the common categories
         */
		Map output = result;
		Assert.assertNotNull( output );
		Assert.assertEquals( states.length, output.size() );
		List node = (List)output.get( "NY" );
		Assert.assertEquals( 1000 + cities.length /* for rollups*/, node.size() );
		Assert.assertSame( TestStruct.class, node.get( 0 ).getClass() );
    }
   


	void commonAssertions(Map result, Class detailClass, File jsonDump)
	throws Exception
	{

		Map output = result;
		Assert.assertNotNull( output );
		Assert.assertEquals( states.length, output.size() );
		Map node = (Map)output.get( "NY" );
		Assert.assertNotNull(node);
		Map ny = node;
		Assert.assertEquals( cities.length, ny.size() );
		
		node = (Map)output.get( "NJ" );
		Map nj = node;
		Assert.assertEquals( cities.length, nj.size() );

		Stream.of( "CITY01", "CITY02", "CITY03" ).forEach( s -> {
			String cityLabel = "NY|" + s;
			Map innerNode = (Map)ny.get( cityLabel );
			Assert.assertEquals( s, zips.length, innerNode.size() );
		} );

		Stream.of( "CITY01", "CITY02", "CITY03" ).forEach( s -> {
			String cityLabel = "NJ|" + s;
			Map innerNode = (Map)nj.get( cityLabel );
			Assert.assertEquals( s, zips.length, innerNode.size() );
		} );

		Map city01 = (Map)ny.get( "NY|CITY01" );
		List zip00001 = (List)city01.get( "00001" );
		Assert.assertEquals( secs.length, zip00001.size() );
		Assert.assertEquals( detailClass, zip00001.get( 0 ).getClass() );
		Assert.assertEquals( detailClass, zip00001.get( 1 ).getClass() );

		dumpToJsonFile( output, jsonDump );
	}
	
	
	void assertRollupEquals(String msgBase, Object rollup, Class detailClass, String targetValue)
	throws AssertionError
	{
		Assert.assertNotNull(msgBase + " present", rollup);
		Assert.assertEquals(msgBase + " class", detailClass, rollup.getClass());
		if (detailClass == TestStruct.class)
		{
			Assert.assertEquals(msgBase + " value", targetValue, ((TestStruct)rollup).detail.security);
		}
		else
		{
			Assert.assertEquals(msgBase + " value", targetValue, ((TestStruct.Detail)rollup).security);
		}
	}

	class CustomNullSerializer
	extends StdSerializer<Object>
	{
		public CustomNullSerializer()
		{
			super(Object.class);
		}
		
	    @Override
	    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
	    	gen.writeString( "" ) ;
	    }

	    @Override
	    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider serializers,
	            TypeSerializer typeSer)
	        throws IOException
	    {
	    	gen.writeString( "" ) ;
	    }
	}

	class TestStructAccessor
	implements Accessor<TestStruct>
	{

		@Override
		public Object[] getCategoryValues( TestStruct row )
		{
			// TODO Auto-generated method stub
			return new Object[] { row.state, row.city, row.zip };
		}

		@Override
		public Object getDetail( TestStruct row )
		{
			return row;
		}
		
	}

	class TestStructJumbledAccessor
	extends TestStructAccessor
	{

		@Override
		public Object[] getCategoryValues( TestStruct row )
		{
			// TODO Auto-generated method stub
			return new Object[] { row.zip, row.state, row.city };
		}

		@Override
		public Object getDetail( TestStruct row )
		{
			return row;
		}
		
	}
	
	class TestStructAccessorWithDetail
	extends TestStructAccessor
	{

		@Override
		public Object[] getCategoryValues( TestStruct row )
		{
			// TODO Auto-generated method stub
			return new Object[] { row.state, row.city, row.zip } ;
		}

		@Override
		public Object getDetail( TestStruct row )
		{
			return row.getDetail();
		}
		
	}

	class TestStructAccessorShort //  
    implements Accessor<TestStruct>
    {   

        @Override
        public Object[] getCategoryValues( TestStruct row )
        {
            // TODO Auto-generated method stub
            // return new Object[] { row.state, row.city, row.zip };
            return new Object[] { row.state };    
        }

        @Override
        public Object getDetail( TestStruct row )
        {
            return row;
        }
    
    }
	
	private void dumpToJsonFile(Object object, File jsonDump) throws Exception
	{
		if (jsonDump == null)
		{
			return;
		}
		ObjectMapper ser = new ObjectMapper();
		SimpleModule s = new SimpleModule();
		s.addSerializer( Object.class, new CustomNullSerializer() );
		ser.registerModule( s );
		String json = ser.writeValueAsString( object );

		File tmpDir = new File(System.getProperty( "java.io.tmpdir" ));
		log.info( "Java_tmp_dir={}", tmpDir);
		try (BufferedWriter wrt = new BufferedWriter( new FileWriter(jsonDump))) 
		{
			wrt.append( json );
			wrt.flush();
		}
		catch(Exception e)
		{
			log.error( "failed to write JSON", e );
		}

	}


}
