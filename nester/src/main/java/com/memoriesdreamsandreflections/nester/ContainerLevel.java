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


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple greeter class.
 *
 * @author saden
 */
@SuppressWarnings( { "rawtypes" } )

public abstract class ContainerLevel<T>
{
	CategoryDescriptor<T> descriptor;
	Object lastValue;
	List container;
	boolean valueBreak;
	boolean reset;

	public ContainerLevel( CategoryDescriptor<T> descriptor ) throws NoSuchFieldException, SecurityException
	{
		this.descriptor = descriptor;
	}

	public Object getLastValue()
	{
		return lastValue;
	}

	public void setLastValue( Object lastValue )
	{
		this.lastValue = lastValue;
	}

	public Field getCategoryField()
	{
		return descriptor.getCategoryField();
	}

	public boolean isValueBreak()
	{
		return valueBreak;
	}

	public void setValueBreak( boolean valueBreak )
	{
		this.valueBreak = valueBreak;
	}

	public List getContainer()
	{
		return container;
	}

	public void setContainer( List container )
	{
		this.container = container;
	}

	public Object getAndTestFieldValue( T row )
	throws IllegalAccessException
	{
		Object fieldValue = this.getCategoryField().get( row );
		if ( !Objects.equals( fieldValue, lastValue ) || reset )
		{
			valueBreak = true;
			lastValue = fieldValue;
		}
		else
		{
			valueBreak = false;
		}
		reset = false;
		return fieldValue;
	}

	public void testFieldValue( Object fieldValue )
	throws IllegalAccessException
	{
		if ( !Objects.equals( fieldValue, lastValue ) || reset )
		{
			valueBreak = true;
			lastValue = fieldValue;
		}
		else
		{
			valueBreak = false;
		}
		reset = false;
	}

	public List newContainer()
	{
		return (container = new ArrayList(2));
	}

	public void reset()
	{
		container = null;
		lastValue = null;
		reset = true;
	}

}
