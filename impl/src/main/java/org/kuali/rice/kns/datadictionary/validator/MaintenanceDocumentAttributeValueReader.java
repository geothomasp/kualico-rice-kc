/*
 * Copyright 2011 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.kns.datadictionary.validator;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.kuali.rice.core.util.type.TypeUtils;
import org.kuali.rice.kns.datadictionary.AttributeDefinition;
import org.kuali.rice.kns.datadictionary.MaintainableFieldDefinition;
import org.kuali.rice.kns.datadictionary.MaintainableItemDefinition;
import org.kuali.rice.kns.datadictionary.MaintainableSectionDefinition;
import org.kuali.rice.kns.datadictionary.MaintenanceDocumentEntry;
import org.kuali.rice.kns.dto.Validatable;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.kns.service.PersistenceStructureService;
import org.kuali.rice.kns.util.ObjectUtils;

/**
 * 
 * @author James Renfro, University of Washington 
 */
public class MaintenanceDocumentAttributeValueReader extends DictionaryObjectAttributeValueReader {

	private final static Logger LOG = Logger.getLogger(MaintenanceDocumentAttributeValueReader.class);
	
	private List<Validatable> attributeDefinitions;
	private Map<String, Validatable> attributeDefinitionMap;
	
	public MaintenanceDocumentAttributeValueReader(Object object, String entryName, MaintenanceDocumentEntry entry, PersistenceStructureService persistenceStructureService) {
		super(object, entryName, entry);
		
		this.attributeDefinitions = new LinkedList<Validatable>();
		this.attributeDefinitionMap = new HashMap<String, Validatable>();
		for (MaintainableSectionDefinition sectionDefinition : entry.getMaintainableSections()) {
			List<? extends MaintainableItemDefinition> itemDefinitions = sectionDefinition.getMaintainableItems();
			
			for (MaintainableItemDefinition itemDefinition : itemDefinitions) {
				if (itemDefinition instanceof MaintainableFieldDefinition) {
					String attributeName = itemDefinition.getName();
					AttributeDefinition attributeDefinition = KNSServiceLocator.getDataDictionaryService().getAttributeDefinition(object.getClass().getName(), attributeName);
						
						//entry.getAttributeDefinition(attributeName);
					boolean isAttributeDefined = attributeDefinition != null;
						//getDataDictionaryService().isAttributeDefined(businessObject.getClass(), itemDefinition.getName());
			        if (isAttributeDefined) {
			        	attributeDefinitions.add(attributeDefinition);
			        	attributeDefinitionMap.put(attributeName, attributeDefinition);
			        	PropertyDescriptor propertyDescriptor = beanInfo.get(attributeName);
			    		Method readMethod = propertyDescriptor.getReadMethod();
			    		
						try {
							Object attributeValue = readMethod.invoke(object);
							
							if (attributeValue != null && StringUtils.isNotBlank(attributeValue.toString())) {
				    			Class<?> propertyType = ObjectUtils.getPropertyType(object, attributeName, persistenceStructureService);
				    			attributeTypeMap.put(attributeName, propertyType);
				    			if (TypeUtils.isStringClass(propertyType) || TypeUtils.isIntegralClass(propertyType) || TypeUtils.isDecimalClass(propertyType) || TypeUtils.isTemporalClass(propertyType)) {
					                // check value format against dictionary
				                    if (!TypeUtils.isTemporalClass(propertyType)) {
				                    	attributeValueMap.put(attributeName, attributeValue);
				                    }
					            }
				    		}
						} catch (IllegalArgumentException e) {
							LOG.warn("Failed to invoke read method on object when looking for " + attributeName + " as a field of " + entry.getDocumentTypeName(), e);
						} catch (IllegalAccessException e) {
							LOG.warn("Failed to invoke read method on object when looking for " + attributeName + " as a field of " + entry.getDocumentTypeName(), e);
						} catch (InvocationTargetException e) {
							LOG.warn("Failed to invoke read method on object when looking for " + attributeName + " as a field of " + entry.getDocumentTypeName(), e);						
			        	}
			    		
			        }
				}
			}
		}
	}
	
	/**
	 * @see org.kuali.rice.kns.datadictionary.validator.AttributeValueReader#getDefinition(java.lang.String)
	 */
	@Override
	public Validatable getDefinition(String attributeName) {
		return attributeDefinitionMap != null ? attributeDefinitionMap.get(attributeName) : null;
	}

	/**
	 * @see org.kuali.rice.kns.datadictionary.validator.AttributeValueReader#getDefinitions()
	 */
	@Override
	public List<Validatable> getDefinitions() {
		return attributeDefinitions;
	}

	/**
	 * @see org.kuali.rice.kns.datadictionary.validator.AttributeValueReader#getType(java.lang.String)
	 */
	@Override
	public Class<?> getType(String attributeName) {
		return attributeTypeMap != null ? attributeTypeMap.get(attributeName) : null;
	}

	/**
	 * @see org.kuali.rice.kns.datadictionary.validator.AttributeValueReader#getValue(java.lang.String)
	 */
	@Override
	public <X> X getValue(String attributeName) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		return (X) attributeValueMap.get(attributeName);
	}

}
