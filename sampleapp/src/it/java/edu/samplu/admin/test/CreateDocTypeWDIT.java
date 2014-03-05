/**
 * Copyright 2005-2011 The Kuali Foundation
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
package edu.samplu.admin.test;

import org.junit.Test;

import edu.samplu.common.ITUtil;
import edu.samplu.common.WebDriverLegacyITBase;

/**
 * Create a document type via the document type creation screen and verify it in docsearch
 * 
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class CreateDocTypeWDIT extends WebDriverLegacyITBase{
    
    public static final String TEST_URL=ITUtil.PORTAL+"?channelTitle=Document%20Type&channelUrl="+ITUtil.getBaseUrlString()+"/kr/lookup.do?methodToCall=start&businessObjectClassName=org.kuali.rice.kew.doctype.bo.DocumentType&returnLocation="+ITUtil.PORTAL_URL+"&hideReturnLink=true&docFormKey=88888888";
    
    /**
     * This overridden method ...
     * 
     * @see edu.samplu.common.WebDriverLegacyITBase#getTestUrl()
     */
    @Override
    public String getTestUrl() {
        // TODO dmoteria - THIS METHOD NEEDS JAVADOCS
        return TEST_URL;
    }
   
    @Test
    public void testCreateDocType() throws Exception {
        super.testCreateDocType();
    }
    
}