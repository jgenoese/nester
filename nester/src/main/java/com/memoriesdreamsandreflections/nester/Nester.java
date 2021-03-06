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


import java.io.Serializable ;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap ;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects ;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nested Map builder
 * 
 * Builds nested Map from input data based on categories.
 *
 * @author jgenoese (BlueSky) pmengaziol (BlueSky)
 */
@SuppressWarnings( { "rawtypes" } )
public class Nester<R>
{
	static Logger log = LoggerFactory.getLogger( Nester.class );
	List<CategoryDescriptor<R>> categoryDescriptors;
	Object[] categoryValues;
	List<ContainerLevel> fields = null; // for reflection
	String detailFieldName;
	Field detailField;
	Class<R> rowClazz;
	Accessor<R> accessor;
	int inmostLevel;
	boolean ordered;
	long nanos;
	long[] levelClocks;
	boolean collectDetails = true;
	Map nameValueRoot;
	boolean queryOnly;

	public static <R> Nester<R> newInstance( Class<R> rowClass )
	{
		Nester<R> nester = new Nester<R>();
		nester.rowClazz = rowClass;
		return nester;
	}
	
	@SuppressWarnings( "unchecked" )
	public static Nester getRehydratedInstance(Map dehydrated)
	{
		if (!dehydrated.containsKey( "specs" ) || !dehydrated.containsKey( "root" ))
		{
			throw new IllegalArgumentException("invalid dehydrated instance");
		}

		Nester result = new Nester(dehydrated);
		return result;
	}

	protected Nester()
	{
	}

	@SuppressWarnings( "unchecked" )
	protected Nester(Map dehydrated)
	{
		try
		{
			nameValueRoot = (Map)dehydrated.get( "root" );
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("invalid dehydrated instance", e);
		}
		
		try
		{
			List<Map<String,String>>specList = (List<Map<String,String>>)dehydrated.get( "specs" );
			if (specList == null || specList.size() == 0)
			{
				new IllegalArgumentException("invalid dehydrated instance: specs");
			}
			categoryDescriptors = new ArrayList<>(specList.size());
			inmostLevel = categoryDescriptors.size() - 1;

			for (Map<String, String> spec : specList)
			{
				categoryDescriptors.add( CategoryDescriptor.newQueryInstance(spec));
			}
			inmostLevel = categoryDescriptors.size() - 1;
		}
		catch(IllegalArgumentException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw new IllegalArgumentException("invalid dehydrated instance", e);
		}
		queryOnly = true;
	}
	
	public Map<?,?> getDehydratedInstance()
	{
		if (nameValueRoot == null)
		{
			throw new UnsupportedOperationException("no result yet");
		}
		Map<String,Object> result = new LinkedHashMap<>(2);
		List<Map<String,String>>specList = new ArrayList<>(categoryDescriptors.size());
		result.put( "specs", specList );
		result.put( "root", nameValueRoot );
		for (CategoryDescriptor< ? > desc : categoryDescriptors)
		{
			specList.add( desc.specMap() );
		}
		return result;
	}

	public Nester<R> categoryDescriptors( List<CategoryDescriptor<R>> categoryDescriptors )
	throws NoSuchFieldException, SecurityException
	{
		this.categoryDescriptors = categoryDescriptors;
		inmostLevel = categoryDescriptors.size() - 1;
		for (CategoryDescriptor desc : categoryDescriptors)
		{
			desc.initializeFields() ;
		}
		initializeFields();
		levelClocks = new long[ categoryDescriptors.size() ];
		return this;
	}

	public Nester<R> detailFieldName( String detailFieldName )
	throws NoSuchFieldException, SecurityException
	{
		if ( accessor != null )
		{
			log.warn( "When both accessor() and detailFieldName() are specified, accessor() takes precedence." );
			this.detailFieldName = null;
			this.detailField = null;
			return this;
		}
		this.detailFieldName = detailFieldName;
		initializeDetailField();
		return this;
	}

	public Nester<R> ordered()
	throws NoSuchFieldException, SecurityException
	{
		this.ordered = true;
		return this;
	}

	public Nester<R> accessor( Accessor<R> accessor )
	{
		if ( detailFieldName != null )
		{
			log.warn( "When both accessor() and detailFieldName() are specified, accessor() takes precedence." );
		}
		this.accessor = accessor;
		this.detailField = null;
		this.detailFieldName = null;
		return this;
	}

	public Nester<R> collectDetails(boolean value)
	{
		this.collectDetails = value;
		return this;
	}

	public Map nest( List<R> rows )
	throws Exception
	{
		nanos = System.nanoTime();
		if ( accessor == null )
		{
			accessor = new DefaultAccessor( fields, detailField );
		}

		Map masterNode = new LinkedHashMap<Object, Object>();
		
		if ( ordered )
		{
			rows.stream().forEach( row -> {
				try
				{
					processOrderedRow( row, masterNode );
				}
				catch ( Exception e )
				{
					log.error( "processUnorderedRow error,", e );
				}
			} );
		}
		else
		{
			rows.stream().forEach( row -> {
				try
				{
					processUnorderedRow( row, masterNode );
				}
				catch ( Exception e )
				{
					log.error( "processUnorderedRow error,", e );
					throw new RuntimeException(e);
				}
			} );
		}

		nanos = System.nanoTime() - nanos;

		return masterNode;

	}

	@SuppressWarnings( "unchecked" )
	public Map<?,?> nameValueNest(List<R> rows) throws IllegalArgumentException, IllegalAccessException
	{
		nanos = System.nanoTime();
		if ( accessor == null )
		{
			accessor = new DefaultAccessor( fields, detailField );
		}
		
		CategoryDescriptor<R> top = categoryDescriptors.get(0);
		
		nameValueRoot = new HashMap<>();
//		NavHelper navRoot = new NavHelper(0, null, 0 == inmostLevel);
		nameValueRoot.put( top.getCollectionLabel(), new ArrayList<Map>() );
	
		for (R row : rows)
		{
			processUnorderedNameValueMapRow(row, nameValueRoot); 
		}
		nanos = System.nanoTime() - nanos;
		
		return nameValueRoot;
	}
	
	public Object query(Object...queryKeyValues )
	{
		Object result = null; 
		if (nameValueRoot == null)
		{
			throw new UnsupportedOperationException();
		}
		if (queryKeyValues != null && queryKeyValues.length > categoryDescriptors.size())
		{
			throw new IllegalArgumentException();
		}
		if (queryKeyValues == null || queryKeyValues.length == 0)
		{
			return nameValueRoot;
		}
		List parent = (List)nameValueRoot.get( categoryDescriptors.get( 0 ).getCollectionLabel() );
		for (int i=0; i<queryKeyValues.length; i++)
		{
			result = lookupInParent(parent, queryKeyValues[i], categoryDescriptors.get( i ));
			if (result == null)
			{
				return null; // you have your answer -- NO RESULTS FOR QUERY
			}
			if (i < inmostLevel)
			{
				parent = (List)((Map)result).get( categoryDescriptors.get( i+1 ).getCollectionLabel() );
			}
		}
		return result;
	}
	
	private Object lookupInParent(List parent, Object queryKeyValue, CategoryDescriptor<R> categoryDescriptor)
	{
		Object storedKey = null;
		for(Object element : parent)
		{
			if (element instanceof Map)
			{
				storedKey = ((Map)element).get( categoryDescriptor.renderedCategoryFieldName() );
			}
			else
			{
				storedKey = element;
			}
			if (Objects.equals( queryKeyValue, storedKey ) )
			{
				return element;
			}
		}
		return null;
	}
	
	
	
	class NavHelper
	{
		int idx;
		Object key;
		Map navMap;
		
		public NavHelper(int idx, Object key, boolean lowest)
		{
			this.idx = idx;
			this.key = key;
			this.navMap = new HashMap<>();
		}
	}
	
	@SuppressWarnings( "unchecked" )
	protected void processUnorderedNameValueMapRow(R row, Map root) throws IllegalArgumentException, IllegalAccessException
	{
		long clock = System.nanoTime();
		Object categoryValue = null;
		Map parent = root;
		List listForCurrentCatDesc = null;
		for ( int i = 0; i < categoryDescriptors.size(); i++ )
		{
			clock = System.nanoTime();
			CategoryDescriptor<R> desc = categoryDescriptors.get( i );
			categoryValue = desc.getCategoryField().get( row );
			if (categoryValue == null)
			{
				return; // no rollups for now
			}
			
			/*
			 * The parent is actually one level above the current category. Hence, 
			 * the list returned by 'parent.get(desc.getCollectionLabel())' is either 
			 * the list of maps for all this category's values, or (if inmostLevel and 
			 * not collecting details) the list of simple category values.
			 */
			listForCurrentCatDesc = (List)parent.get(desc.getCollectionLabel());
			
			
			Object target = lookup( desc, categoryValue, listForCurrentCatDesc, i == inmostLevel );
			if ( target == null )
			{
				try
				{
					if ( i != inmostLevel)
					{
						Map newMapForCategoryValue = new LinkedHashMap();
						List newListForNextLowerCategory = new ArrayList();
						newMapForCategoryValue.putAll(desc.mapCatAndAncFields( row )); // will include category Value, of course
						newMapForCategoryValue.put( categoryDescriptors.get(i+1).getCollectionLabel(), newListForNextLowerCategory );
						listForCurrentCatDesc.add( newMapForCategoryValue );
						
						parent = newMapForCategoryValue;
					}
					else
					{
						if ( desc.categoryRequiresListOfMaps() )
						{
							Map newMapForCategoryValue = new LinkedHashMap();
							newMapForCategoryValue.putAll(desc.mapCatAndAncFields( row )); // will include category Value, of course
							if (desc.getDetailCollectionLabel() != null)
							{
								List newListForDetailMaps = new ArrayList();
								newMapForCategoryValue.put( desc.getDetailCollectionLabel(), newListForDetailMaps );
								newListForDetailMaps.add( desc.mapDetail( row ) );
							}
							listForCurrentCatDesc.add( newMapForCategoryValue );
						}
						else
						{
							listForCurrentCatDesc.add(categoryValue);
						}
					}
				}
				catch ( Exception e )
				{
					log.error( "Exception processing row", e );
					throw new RuntimeException(e);
				}
				finally
				{
					clock = System.nanoTime();
					levelClocks[ i ] += System.nanoTime() - clock;
				}
			}
			else // target found
			{
				/*
				 * Inmost level contains
				 * 		either a List of category value objects 
				 * 			(when desc.detailCollectionLabel is null)
				 * 		or a List of Maps containing key/value pairs
				 * 		{
				 * 			categoryFieldName: categoryFieldValue,
				 * 			detailCollectionLabel : [
				 * 					list of detail maps
				 * 			]
				 * 		}
				 * 
				 * Every other level contains key/value pairs
				 * 		{
				 * 			categoryFieldName: categoryFieldValue,
				 * 			collectionLabel (of next level) : [
				 * 					list of category maps
				 * 			]
				 * 		}
				 */

				if ( i == inmostLevel )
				{
					if (desc.categoryRequiresListOfMaps())
					{
						Map targetMap = (Map)target;
						List targetList = (List)targetMap.get( desc.getDetailCollectionLabel() );
						targetList.add( desc.mapDetail( row ) );
					}
				}
				else
				{
					parent = (Map)target;
				}
			}
		}

		
	}
	
	
	private Object lookup( CategoryDescriptor<R> desc, Object categoryValue, List parentList, boolean atInmostLevel )
	{
		/*
		 * Inmost level contains
		 * 		either a List of category value objects 
		 * 			(when desc.detailCollectionLabel is null)
		 * 		or a List of Maps containing key/value pairs
		 * 		{
		 * 			categoryFieldName: categoryFieldValue,
		 * 			detailCollectionLabel : [
		 * 					list of detail maps
		 * 			]
		 * 		}
		 * 
		 * Every other level contains key/value pairs
		 * 		{
		 * 			categoryFieldName: categoryFieldValue,
		 * 			collectionLabel (of next level) : [
		 * 					list of category maps
		 * 			]
		 * 		}
		 */
		if (!atInmostLevel || desc.getDetailCollectionLabel() != null)
		{
			Map possibleTargetMap = null;
			Object possibleTargetCategoryValue = null;
			
			for (int i=0; i<parentList.size(); i++)
			{
				possibleTargetMap = (Map)parentList.get( i );
				possibleTargetCategoryValue = possibleTargetMap.get(desc.renderedCategoryFieldName());
				if (categoryValue.equals( possibleTargetCategoryValue ))
				{
					return possibleTargetMap; // we found it
				}
			}
			return null;
		}
		else
		{
			for (int i=0; i<parentList.size(); i++)
			{
				if (categoryValue.equals( parentList.get( i ) ))
				{
					return parentList.get( i );
				}
			}
			return null;
		}
	}

	public long getNanos()
	{
		return nanos;
	}

	public long[] getLevelClocks()
	{
		return levelClocks;
	}

	@SuppressWarnings( { "unchecked" } )
	protected void processUnorderedRow( R row, Map masterNode )
	throws Exception
	{
		ListLevel listHelper = new ListLevel(null); 
		MapLevel mapHelper = new MapLevel(null); 
		Map<Object, Object> parentNode = masterNode;
		Map<Object, Object> parent = masterNode;
		Object categoryValue = null;
		long clock = 0l;

		Object[] categoryValues = accessor.getCategoryValues( row );

		for ( int i = 0; i < fields.size(); i++ )
		{
			clock = System.nanoTime();
			parent = parentNode;
			categoryValue = categoryValues[ i ];
			Object objectForCurrentCatValue = parent == null ? null : parent.get( categoryValue );
			if ( objectForCurrentCatValue == null )
			{
				try
				{
					if ( i == inmostLevel )
					{
						List l = listHelper.newContainer();
						if (parent != null)
						{
							parent.put( categoryValue, l );
						}
						listHelper.addDetail( l, row );
					}
					else
					{
						Map m = mapHelper.newContainer();
						if (parent != null)
						{
							parent.put( categoryValue, m );
						}
						parentNode = m;
					}
				}
				catch ( Exception e )
				{
					log.error( "Exception processing row", e );
				}
				finally
				{
					clock = System.nanoTime();
					levelClocks[ i ] += System.nanoTime() - clock;
				}
			}
			else
			{
				if ( i == inmostLevel )
				{
					List l = (List)objectForCurrentCatValue;
					listHelper.addDetail( l, row );
				}
				else
				{
					parentNode = (Map)objectForCurrentCatValue;
				}
			}
		}

	}


	@SuppressWarnings( { "unchecked" } )
	protected void processOrderedRow( R row, Map<?,?> masterNode )
	throws Exception
	{
		Map<?,?> parentNode = masterNode; 
		Map parent = null;
		Object categoryValue = null;
		long clock = 0l;

		Object[] categoryValues = accessor.getCategoryValues( row );

		for ( int categoryIdx = 0; categoryIdx < fields.size(); categoryIdx++ )
		{
			clock = System.nanoTime();
			parent = parentNode;
			categoryValue = categoryValues[ categoryIdx ];
			ContainerLevel cl = fields.get( categoryIdx );
			try
			{
				cl.testFieldValue( categoryValue );
				if ( categoryIdx == inmostLevel )
				{
					ListLevel ll = (ListLevel) cl;
					if ( !ll.isValueBreak() )
					{
						if (collectDetails)
						{
							ll.getList().add( accessor.getDetail( row ) );
						}
					}
					else
					{
						ll.newContainer();
						List list = ll.getList();
						parent.put( categoryValue, ll.getContainer() );
						if (collectDetails)
						{
							list.add( accessor.getDetail( row ) );
						}
					}
				}
				else
				{
					MapLevel ml = (MapLevel) cl;
					if ( ml.isValueBreak() )
					{
						ml.newContainer();
						parent.put( categoryValue, ml.getContainer() );
						for ( int j = categoryIdx + 1; j <= inmostLevel; j++ )
						{
							fields.get( j ).reset();
						}
					}
					parentNode = ml.getContainer();
				}
			}
			catch ( Exception e )
			{
				log.error( "Exception processing row", e );
			}
			levelClocks[ categoryIdx ] += System.nanoTime() - clock;
		}

	}

	private void initializeFields()
	throws NoSuchFieldException, SecurityException
	{
		fields = categoryDescriptors.subList( 0, categoryDescriptors.size() - 1 ).stream().map( s -> field( s ) ).collect( Collectors.toList() );

		fields.add( new ListLevel( categoryDescriptors.get( inmostLevel ) ) );
	}

	private void initializeDetailField()
	throws NoSuchFieldException, SecurityException
	{
		detailField = rowClazz.getDeclaredField( detailFieldName );
	}

	private ContainerLevel field( CategoryDescriptor<R> descriptor )
	{
		try
		{
			return new MapLevel( descriptor );
		}
		catch ( Exception e )
		{
			log.error( descriptor.getCategoryField().getName() + " is not a valid category descriptor.", e );
			return null;
		}
	}
	
	List list(List node)
	{
		return (List)node.get( node.size() - 1);
	}
	
	Map map(List node)
	{
		return (Map)node.get( node.size() - 1);
	}

	class MapLevel
	extends ContainerLevel<R, Map>
	{
		
		public MapLevel( CategoryDescriptor<R> descriptor ) throws NoSuchFieldException, SecurityException
		{
			super( descriptor );
		}

		@Override
		public Map< ? , ? > newContainer()
		{
			return (container = new LinkedHashMap<Object, Object>() );
		}
		
		public Map< ? , ? > getMap()
		{
			return container;
		}
	}

	class ListLevel
	extends ContainerLevel<R, List>
	{
		public ListLevel( CategoryDescriptor<R> descriptor ) throws NoSuchFieldException, SecurityException
		{
			super( descriptor );
		}

		@SuppressWarnings( "unchecked" )
		@Override
		public List<Serializable> newContainer()
		{
			return (container = new ArrayList());
		}
		
		public List getList()
		{
			return container;
		}
		
		@SuppressWarnings( "unchecked" )
		public void addDetail(List node, R row)
		{
			if (collectDetails)
			{
				node.add( accessor.getDetail( row ) );
			}
		}
	}

	class DefaultAccessor
	implements Accessor<R>
	{
		Field[] rowFields;
		Field detailField;
		Function<R, Object> detailAccessor;

		public DefaultAccessor( List<ContainerLevel> fields, Field detailField )
		{
			rowFields = new Field[ fields.size() ];
			for ( int i = 0; i < rowFields.length; i++ )
			{
				rowFields[ i ] = fields.get( i ).getCategoryField();
			}
			this.detailField = detailField;
			detailAccessor = ( detailField == null ) ?
				new Function<R, Object>()
				{
					public R apply( R row )
					{
						return row;
					}
				} 
				: 
				new Function<R, Object>()
				{
					public Object apply( R row )
					{
						try
						{
							return (Object)detailField.get( row );
						}
						catch ( Exception e )
						{
							log.error( "Field access error", e );
							return null;
						}
					}
				}
			;

		}

		@Override
		public Object[] getCategoryValues( R row )
		{
			Object[] results = new Object[ rowFields.length ];
			for ( int i = 0; i < rowFields.length; i++ )
			{
				try
				{
					results[ i ] = rowFields[ i ].get( row );
				}
				catch ( Exception e )
				{
					log.error( "Field access error", e );
				}
			}
			return results;
		}

		@Override
		public Object getDetail( R row )
		{
			return detailAccessor.apply( row );
		}

	}

}
