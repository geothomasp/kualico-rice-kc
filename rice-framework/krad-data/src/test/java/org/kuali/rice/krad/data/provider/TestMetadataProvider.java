/**
 * Copyright 2005-2015 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.krad.data.provider;

import java.util.Collection;
import java.util.Map;

import org.kuali.rice.krad.data.metadata.DataObjectMetadata;

/**
 */
public class TestMetadataProvider implements MetadataProvider {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
	public Map<Class<?>, DataObjectMetadata> provideMetadata() {
        return null;
    }

    @Override
    public DataObjectMetadata getMetadataForType(Class<?> dataObjectType) throws IllegalArgumentException {
        return null;
    }

    @Override
    public boolean handles(Class<?> type) {
        return true;
    }

    @Override
	public Collection<Class<?>> getSupportedTypes() {
        return null;
    }

	@Override
	public boolean requiresListOfExistingTypes() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<Class<?>, DataObjectMetadata> provideMetadataForTypes(Collection<Class<?>> types) {
		// TODO Auto-generated method stub
		return null;
	}
}