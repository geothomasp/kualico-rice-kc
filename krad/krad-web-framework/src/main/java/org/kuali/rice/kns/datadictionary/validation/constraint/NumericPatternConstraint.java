/*
 * Copyright 2005-2007 The Kuali Foundation
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
package org.kuali.rice.kns.datadictionary.validation.constraint;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.lang.StringUtils;


/**
 * Pattern for matching numeric characters
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class NumericPatternConstraint extends ValidCharactersPatternConstraint {
    /**
     * @see org.kuali.rice.kns.datadictionary.validation.ValidationPattern#getRegexString()
     */
    protected String getRegexString() {
        return "[0-9]";
    }

	/**
	 * This overridden method ...
	 * 
	 * @see org.kuali.rice.kns.datadictionary.validation.constraint.BaseConstraint#getLabelKey()
	 */
	@Override
	public String getLabelKey() {
		String labelKey = super.getLabelKey();
		if (StringUtils.isNotEmpty(labelKey)) {
			return labelKey;
		}
		StringBuilder key = new StringBuilder("");
		key.append("numericPattern,");
		return key.toString();
	}

}
