package com.memoriesdreamsandreflections.nester;

import java.lang.reflect.Field ;
import java.util.LinkedHashMap ;
import java.util.Map ;
import java.util.function.Function ;

public class CategoryDescriptor<T>
{
	Class<T> clazz;
	String collectionLabel;
	String detailCollectionLabel;
	String categoryFieldName;
	String categoryFieldRenderedName;
	String[] ancillaryFieldNames;
	Function<T, Map<String, Object>> ancillaryValueMappingFunction;
	Function<T, Map<String, Object>> detailValueMappingFunction;
	Field categoryfield;
	Field[] ancillaryFields;
	boolean hasAncillaryFields;
	
	/*
	 * Query fields
	 *
	collectionLabel
	categoryFieldName
	categoryFieldRenderedName
	 */
	
	
	protected CategoryDescriptor()
	{
		
	}
	
	protected CategoryDescriptor(Class<T> clazz)
	{
		this.clazz = clazz;
	}
	
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public static CategoryDescriptor newQueryInstance(Map<String, String> spec)
	{
		CategoryDescriptor desc = new CategoryDescriptor();
		desc.collectionLabel = spec.get( "collectionLabel" );
		desc.categoryFieldName = spec.get( "renderedCategoryFieldName" );
		return desc;
	}
	
	public static <T> CategoryDescriptor<T> newInstance(Class<T> clazz)
	{
		return new CategoryDescriptor<T>(clazz);
	}
	
	public CategoryDescriptor<T> withCollectionLabel(String value)
	{
		this.collectionLabel = value;
		return this;
	}
	
	/**
	 * For innermost nested level category only, detail collection key value
	 * @param value
	 * @return this
	 */
	public CategoryDescriptor<T> withDetailCollectionLabel(String value)
	{
		this.detailCollectionLabel = value;
		return this;
	}
	
	public CategoryDescriptor<T> withCategoryFieldName(String value)
	{
		this.categoryFieldName = value;
		return this;
	}
	
	public CategoryDescriptor<T> withCategoryFieldRenderedName(String value)
	{
		this.categoryFieldRenderedName = value;
		return this;
	}

	public CategoryDescriptor<T> withAncillaryFieldNames(String[] value)
	{
		this.ancillaryFieldNames = value;
		hasAncillaryFields = ancillaryFieldNames != null && ancillaryFieldNames.length > 0;
		return this;
	}
	
	public CategoryDescriptor<T> withAncillaryValueMappingFunction(Function<T, Map<String, Object>> value)
	{
		this.ancillaryValueMappingFunction = value;
		return this;
	}
	
	/**
	 * Provide a function for mapping details for the current category. Note that only the lowest level of
	 * category will employ this, if present. 
	 * 
	 * Value provided:
	 * The lowest level of category will collect all details in a map
	 * 
	 * Value not provided:
	 * List the category values atomically in a simple list.  
	 * @param value Function instance
	 * @return this
	 */
	public CategoryDescriptor<T> withDetailValueMappingFunction(Function<T, Map<String, Object>> value)
	{
		this.detailValueMappingFunction = value;
		return this;
	}
	
	public void initializeFields() throws NoSuchFieldException, SecurityException
	{
		if (categoryFieldName != null)
		{
			categoryfield = clazz.getDeclaredField( categoryFieldName );
		}
		
		if (ancillaryFieldNames != null && ancillaryFieldNames.length > 0)
		{
			ancillaryFields = new Field[ancillaryFieldNames.length];
			for(int i=0; i<ancillaryFieldNames.length; i++)
			{
				ancillaryFields[i] = clazz.getDeclaredField( ancillaryFieldNames[i] );
			}
		}
	}

	public Class<T> getClazz()
	{
		return clazz ;
	}

	public String getCollectionLabel()
	{
		return collectionLabel ;
	}

	public String getDetailCollectionLabel()
	{
		return detailCollectionLabel ;
	}

	public Field getCategoryField()
	{
		return categoryfield ;
	}

	public Field[] getAncillaryFields()
	{
		return ancillaryFields ;
	}

	/**
	 * Function that will obtain the designated detail structure as name/value map. Note that this
	 * is only relevant to the lowest level of category. 
	 * @param t instance of detail structure
	 * @return Map<String Object>
	 */
	Map<String, Object> mapDetail(T t)
	{
		return detailValueMappingFunction == null ? null : detailValueMappingFunction.apply( t );
	}

	/**
	 * Function that will obtain primary categoryfield and all extra (a/k/a ancillary) fields requested with each unique key at each category level, 
	 * and return them in a Map
	 * <br/>
	 * E.g. 
	 * <ul><li>"state" : "NY",<br/></li>
	 * <li>"stateEmblem" : "vulture"</li></ul>
	 * @param t raw data row|record
	 * @return @{link Map} fieldname/value mapping
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public Map<String, Object> mapAllFields(T t) throws IllegalArgumentException, IllegalAccessException
	{
		
		Map<String, Object> result = new LinkedHashMap<>();
		result.put( renderedCategoryFieldName(), categoryfield.get( t ) );
		if ( hasAncillaryFields )
		{
			for (Field ancField : ancillaryFields)
			{
				result.put( ancField.getName(), ancField.get( t ) );
			}
		}
		if ( ancillaryValueMappingFunction != null )
		{
			result.putAll( ancillaryValueMappingFunction.apply( t ) );
		}
		return result;
	}
	
	/**
	 * Normally, the data stored for the <b>lowest level</b> CategoryDescriptor will be a simple list
	 * of values as opposed to a list of Maps containing key:value pairs.
	 * 
	 * However, when <b>any</b> of the following are defined, a list of Maps containing key:value pairs
	 * will be required:
	 * <ul>
	 * <li> a <i>detailValueMappingFunction</i></li>
	 * <li> an <i>ancillaryValueMappingFunction</i></li>
	 * <li> <i>ancillaryFieldNames</i></li>
	 * </ul>
	 * @return boolean true if the above condition is met.
	 */
	public boolean categoryRequiresListOfMaps()
	{
		return detailValueMappingFunction != null ||
			ancillaryValueMappingFunction != null ||
			(ancillaryFieldNames != null && ancillaryFieldNames.length > 0); 
			
	}
	
	public String renderedCategoryFieldName()
	{
		return categoryFieldRenderedName == null ? categoryFieldName : categoryFieldRenderedName;
	}
	
	public Map<String, String> specMap()
	{
		Map<String,String> spec = new LinkedHashMap<>(2);
		spec.put( "collectionLabel", collectionLabel );
		spec.put( "renderedCategoryFieldName", renderedCategoryFieldName() );
		return spec;
	}
}
