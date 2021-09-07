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

public class TestStruct
{
	String state, city, zip; 
	Detail detail;

	public TestStruct( String state, String city, String zip, String security )
	{
		super();
		this.state = state;
		this.city = city;
		this.zip = zip;
		this.detail = new Detail(security);
	}

	public String getState()
	{
		return state;
	}

	public void setState( String state )
	{
		this.state = state;
	}

	public String getCity()
	{
		return city;
	}

	public void setCity( String city )
	{
		this.city = city;
	}

	public String getZip()
	{
		return zip;
	}

	public void setZip( String zip )
	{
		this.zip = zip;
	}
	
	public Detail getDetail()
	{
		return detail;
	}

	public void setDetail( Detail detail )
	{
		this.detail = detail;
	}

	class Detail
	{
		String security;
		public Detail(String security)
		{
			this.security = security;
		}
		public String getSecurity()
		{
			return security;
		}
		public void setSecurity( String security )
		{
			this.security = security;
		}
		
	}

}
