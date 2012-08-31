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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import edu.samplu.common.AdminMenuBlanketAppITBase;
import edu.samplu.common.AdminMenuITBase;
import edu.samplu.common.ITUtil;
import edu.samplu.common.UpgradedSeleniumITBase;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * tests that user 'admin', on blanket approving a new State maintenance document, results in a final document
 * 
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class LocationStateBlanketAppIT extends AdminMenuBlanketAppITBase {
    
    @Override
    protected String getLinkLocator() {
        return "link=State";
    }

   @Override
   public String blanketApprove() throws Exception {
         
        ITUtil.waitForElement(selenium, AdminMenuITBase.DOC_ID_LOCATOR);
        String docId = selenium.getText(AdminMenuITBase.DOC_ID_LOCATOR);
        
        selenium.type("//input[@id='document.documentHeader.documentDescription']", "Validation Test State");
        //selenium.click("methodToCall.performLookup.(!!org.kuali.rice.location.impl.country.CountryBo!!).(((code:document.newMaintainableObject.countryCode,))).((`document.newMaintainableObject.countryCode:code,`)).((<>)).(([])).((**)).((^^)).((&&)).((//)).((~~)).(::::;" + getBaseUrlString() + "/kr/lookup.do;::::).anchor4");
        
        String countryLookUp = "//input[@name='methodToCall.performLookup.(!!org.kuali.rice.location.impl.country.CountryBo!!).(((code:document.newMaintainableObject.countryCode,))).((`document.newMaintainableObject.countryCode:code,`)).((<>)).(([])).((**)).((^^)).((&&)).((//)).((~~)).(::::;" + getBaseUrlString()+ "/kr/lookup.do;::::).anchor4']";
        for (int second = 0;; second++) {
            if (second >= 60) Assert.fail("timeout");
            try { if (selenium.isElementPresent(countryLookUp)) break; } catch (Exception e) {}
            Thread.sleep(1000);
        }
        selenium.click(countryLookUp);        
        selenium.waitForPageToLoad("30000");
        selenium.click("//input[@name='methodToCall.search' and @value='search']");
        selenium.waitForPageToLoad("30000");
        selenium.click("link=return value");
        selenium.waitForPageToLoad("30000");
        String code = RandomStringUtils.randomAlphabetic(2).toUpperCase();
        selenium.type("//input[@id='document.newMaintainableObject.code']", code);
        String state =  "Validation Test State " + code;
        selenium.type("//input[@id='document.newMaintainableObject.name']",state);
        selenium.click("//input[@id='document.newMaintainableObject.active']");
        
        return docId;
    }
}
 