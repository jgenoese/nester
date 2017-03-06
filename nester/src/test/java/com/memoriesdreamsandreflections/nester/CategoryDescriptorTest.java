package com.memoriesdreamsandreflections.nester;

import static org.junit.Assert.assertEquals ;
import static org.junit.Assert.assertNull ;
import static org.junit.Assert.assertSame ;

import java.lang.reflect.Field ;
import java.util.HashMap ;
import java.util.Map ;
import java.util.function.Function ;

import org.junit.Test ;

public class CategoryDescriptorTest
{

	@Test
	public void testCategoryDescriptor()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = new CategoryDescriptor<TestClass>(clazz); 
		assertSame(clazz, subject.getClazz());
	}

	@Test
	public void testNewInstance()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz); 
		assertSame(clazz, subject.getClazz());
	}

	@Test
	public void testWithCollectionLabel()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withCollectionLabel( "TEST" ); 
		assertSame("TEST", subject.getCollectionLabel());
	}

	@Test
	public void testWithDetailCollectionLabel()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withDetailCollectionLabel( "TEST" ); 
		assertSame("TEST", subject.getDetailCollectionLabel());
	}

	@Test
	public void testWithFieldName()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withCategoryFieldName( "categoryField" ); 
		assertSame("categoryField", subject.categoryFieldName);
	}

	@Test
	public void testWithAncillaryFieldNames()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		String[] ancillaryFieldNames = {"ancillaryField1", "ancillaryField2"};
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withAncillaryFieldNames( ancillaryFieldNames ); 
		assertSame(ancillaryFieldNames, subject.ancillaryFieldNames);
	}

	@Test
	public void testWithAncillaryValueMappingFunction()
	{
		Function<TestClass, Map<String, Object>> f = (t) -> {return new HashMap<>();};
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withAncillaryValueMappingFunction( f ); 
		assertSame(f, subject.ancillaryValueMappingFunction);
	}

	@Test
	public void testWithDetailValueMappingFunction()
	{
		Function<TestClass, Map<String, Object>> f = (t) -> {return new HashMap<>();};
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withDetailValueMappingFunction( f ); 
		assertSame(f, subject.detailValueMappingFunction);
	}

	@Test
	public void testInitializeFields() throws NoSuchFieldException, SecurityException
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		String[] ancillaryFieldNames = {"ancillaryField1", "ancillaryField2"};
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withCategoryFieldName( "categoryField" )
			.withAncillaryFieldNames( ancillaryFieldNames ); 
		subject.initializeFields();
		assertEquals("categoryField", subject.getCategoryField().getName());
		Field[] ancillaryFields = subject.getAncillaryFields();
		assertEquals("ancillaryField1", ancillaryFields[0].getName());
		assertEquals("ancillaryField2", ancillaryFields[1].getName());
		assertEquals(2, ancillaryFields.length);
	}

	@Test
	public void testMapDetail()
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withDetailValueMappingFunction( (t) ->
			{
				Map<String, Object> result = new HashMap<>();
				result.put( "detailField1", t.getDetailField1() );
				result.put( "detailField2", t.getDetailField2() );
				return result;
			}
			);
		TestClass one = new TestClass();
		one.setDetailField1( "1" );
		one.setDetailField2( "2" );
		Map<String, Object> result = subject.mapDetail( one );
		assertEquals(2, result.size());
		assertEquals("1", result.get( "detailField1" ));
		assertEquals("2", result.get( "detailField2" ));
	}

	@Test
	public void testMapAllFields() 
	throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		String[] ancillaryFieldNames = {"ancillaryField1", "ancillaryField2"};
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withCategoryFieldName( "categoryField" )
			.withAncillaryFieldNames( ancillaryFieldNames ); 
		subject.initializeFields();
		TestClass one = new TestClass();
		one.setCategoryField( "fieldValue" );
		one.setAncillaryField1( "ancillaryField1Value" );
		one.setAncillaryField2( "ancillaryField2Value" );
		Map<String, Object> result = subject.mapCatAndAncFields( one );
		assertEquals(3, result.size());
		assertEquals("fieldValue", result.get( "categoryField" ));
		assertEquals("ancillaryField1Value", result.get( "ancillaryField1" ));
		assertEquals("ancillaryField2Value", result.get( "ancillaryField2" ));
	}

	@Test
	public void testMapAllFieldsWithSubstituteCategoryName() 
	throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		@SuppressWarnings( "unchecked" )
		Class<TestClass> clazz = (Class<TestClass>) new TestClass().getClass();
		String[] ancillaryFieldNames = {"ancillaryField1", "ancillaryField2"};
		CategoryDescriptor<TestClass> subject = CategoryDescriptor.newInstance(clazz)
			.withCategoryFieldName( "categoryField" )
			.withCategoryFieldRenderedName( "renderedCategoryField" )
			.withAncillaryFieldNames( ancillaryFieldNames ); 
		subject.initializeFields();
		TestClass one = new TestClass();
		one.setCategoryField( "fieldValue" );
		one.setAncillaryField1( "ancillaryField1Value" );
		one.setAncillaryField2( "ancillaryField2Value" );
		Map<String, Object> result = subject.mapCatAndAncFields( one );
		assertEquals(3, result.size());
		assertNull(result.get( "categoryField" ));
		assertEquals("fieldValue", result.get( "renderedCategoryField" ));
		assertEquals("ancillaryField1Value", result.get( "ancillaryField1" ));
		assertEquals("ancillaryField2Value", result.get( "ancillaryField2" ));
	}
	
	class TestClass
	{
		String categoryField;
		String ancillaryField1;
		String ancillaryField2;
		String detailField1;
		String detailField2;

		public String getCategoryField()
		{
			return categoryField ;
		}
		public void setCategoryField( String categoryField )
		{
			this.categoryField = categoryField ;
		}
		public String getAncillaryField1()
		{
			return ancillaryField1 ;
		}
		public void setAncillaryField1( String ancillaryField1 )
		{
			this.ancillaryField1 = ancillaryField1 ;
		}
		public String getAncillaryField2()
		{
			return ancillaryField2 ;
		}
		public void setAncillaryField2( String ancillaryField2 )
		{
			this.ancillaryField2 = ancillaryField2 ;
		}
		public String getDetailField1()
		{
			return detailField1 ;
		}
		public void setDetailField1( String detailField1 )
		{
			this.detailField1 = detailField1 ;
		}
		public String getDetailField2()
		{
			return detailField2 ;
		}
		public void setDetailField2( String detailField2 )
		{
			this.detailField2 = detailField2 ;
		}
		
	}

}
