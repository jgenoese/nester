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

/**
 * Row accessor, meant to extract values for each category. Used to permit possibility of avoiding reflection, which is
 * still somewhat slow.
 * @author jgenoese
 *
 * @param <R>
 */
public interface Accessor<R>
{
	/**
	 * Obtain values for each category
	 * @param row
	 * @return Array of Objects containing values for each category. The order of elememts must correspond 
	 * exactly to the order of specified categories. Failure to do this will produce "unpredictable results".   
	 */
	public Object[] getCategoryValues(R row);
	
	/**
	 * Return the desired detail value for each row. Ideally, this should be a class member wrapping all those fields
	 * that do not represent categories.
	 * @param row
	 * @return Object containing row detail
	 */
	public Object getDetail(R row);
}
