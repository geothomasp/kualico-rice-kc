/*
 * Copyright 2005-2007 The Kuali Foundation.
 *
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
// Created on Jan 8, 2007

package edu.iu.uis.eden.mail;

import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.kuali.rice.kew.dto.NetworkIdDTO;
import org.kuali.rice.kew.exception.ResourceUnavailableException;
import org.kuali.workflow.test.KEWTestCase;

import edu.iu.uis.eden.KEWServiceLocator;
import edu.iu.uis.eden.actionitem.ActionItem;
import edu.iu.uis.eden.clientapp.WorkflowDocument;
import edu.iu.uis.eden.doctype.DocumentType;
import edu.iu.uis.eden.feedback.web.FeedbackForm;
import edu.iu.uis.eden.plugin.attributes.CustomEmailAttribute;
import edu.iu.uis.eden.user.AuthenticationUserId;
import edu.iu.uis.eden.user.WorkflowUser;

/**
 * Tests email content generation
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 */
public class EmailMessageTest extends KEWTestCase {
    private ActionListEmailServiceImpl actionListEmailService = new ActionListEmailServiceImpl();
    private HardCodedEmailContentServiceImpl hardCodedEmailContentService = new HardCodedEmailContentServiceImpl();
    private StyleableEmailContentServiceImpl styleableContentService = new StyleableEmailContentServiceImpl();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        actionListEmailService.setDeploymentEnvironment("dev");
        hardCodedEmailContentService.setDeploymentEnvironment("dev");
        styleableContentService.setDeploymentEnvironment("dev");
        styleableContentService.setStyleService(KEWServiceLocator.getStyleService());
    }

    @Override
    protected void loadTestData() throws Exception {
        loadXmlFile("EmailMessageDocType.xml");
    }


    private void testImmediateReminder(WorkflowUser user, Collection<ActionItem> actionItems) throws Exception, ResourceUnavailableException {
        for (ActionItem actionItem: actionItems) {
            DocumentType docType = KEWServiceLocator.getDocumentTypeService().findByName(actionItem.getDocName());

            String oldBody = actionListEmailService.buildImmediateReminderBody(user, actionItem, docType);
            CustomEmailAttribute cea = docType.getCustomEmailAttribute();
            // it's not buildImmediateReminderBody, but sendImmediateReminder in the default ActionListEmailServiceImpl
            // that appends the custom content, so we need to do that here in order for the generated content to match
            // that generated by HardCodedEmailContentServiceImpl
            if (cea != null) {
                oldBody += cea.getCustomEmailBody();
            }
            // have to use getEmailSubject("") to simulate generating an email subject with no custom email attribute
            String customSubject = "";
            if (cea != null) {
                customSubject = cea.getCustomEmailSubject();
            }
            String oldSubject = actionListEmailService.getEmailSubject(customSubject).getSubject();

            EmailContent hardCodedContent = hardCodedEmailContentService.generateImmediateReminder(user, actionItem, docType);
            EmailContent styledContent = styleableContentService.generateImmediateReminder(user, actionItem, docType);

            assertEquals("Immediate reminder body is not identical", oldBody, hardCodedContent.getBody());
            assertEquals("Immediate reminder subject is not identical", oldSubject, hardCodedContent.getSubject());
            assertEquals("Immediate reminder body is not identical", oldBody, styledContent.getBody());
            assertEquals("Immediate reminder subject is not identical", oldSubject, styledContent.getSubject());

            log.info("Immediate reminder content: " + styledContent);
        }

    }

    private void testDailyReminder(WorkflowUser user, Collection<ActionItem> actionItems) {
        String oldBody = actionListEmailService.buildDailyReminderBody(user, actionItems);
        String oldSubject = actionListEmailService.getEmailSubject().getSubject();
        EmailContent hardCodedContent = hardCodedEmailContentService.generateDailyReminder(user, actionItems);
        EmailContent styledContent = styleableContentService.generateDailyReminder(user, actionItems);

        assertEquals("Daily reminder body is not identical", oldBody, hardCodedContent.getBody());
        // daily reminder does not use custom email subject
        assertEquals("Daily reminder subject is not identical", oldSubject, hardCodedContent.getSubject());
        assertEquals("Daily reminder body is not identical", oldBody, styledContent.getBody());
        // daily reminder does not use custom email subject
        assertEquals("Daily reminder subject is not identical", oldSubject, styledContent.getSubject());

        log.info("Daily reminder content: " + styledContent);

    }

    private void testWeeklyReminder(WorkflowUser user, Collection<ActionItem> actionItems) {
        String oldBody = actionListEmailService.buildWeeklyReminderBody(user, actionItems);
        String oldSubject = actionListEmailService.getEmailSubject().getSubject();

        EmailContent hardCodedContent = hardCodedEmailContentService.generateWeeklyReminder(user, actionItems);
        EmailContent styledContent = styleableContentService.generateWeeklyReminder(user, actionItems);

        assertEquals("Weekly reminder body is not identical", oldBody, hardCodedContent.getBody());
        // daily reminder does not use custom email subject
        assertEquals("Weekly reminder subject is not identical", oldSubject, hardCodedContent.getSubject());
        assertEquals("Weekly reminder body is not identical", oldBody, styledContent.getBody());
        // daily reminder does not use custom email subject
        assertEquals("Weekly reminder subject is not identical", oldSubject, styledContent.getSubject());

        log.info("Weekly reminder content: " + styledContent);
    }

    private void testEmailContentGeneration(WorkflowUser user, int numItems) throws Exception {
        Collection<ActionItem> actionItems = KEWServiceLocator.getActionListService().getActionList(user, null);
        assertEquals("user should have " + numItems + " items in his action list.", numItems, actionItems.size());

        testImmediateReminder(user, actionItems);

        testDailyReminder(user, actionItems);

        testWeeklyReminder(user, actionItems);
    }

    private int generateDocs(String[] docTypes, WorkflowUser user) throws Exception {
        NetworkIdDTO nid = new NetworkIdDTO(user.getAuthenticationUserId().getId());

        for (String docType: docTypes) {
            WorkflowDocument document = new WorkflowDocument(nid, docType);
            document.setTitle("a title");
            document.routeDocument("");
            document = new WorkflowDocument(nid, docType);
            document.setTitle("a title");
            document.routeDocument("");
            document = new WorkflowDocument(nid, docType);
            document.setTitle("a title");
            document.routeDocument("");
            document = new WorkflowDocument(nid, docType);
            document.setTitle("a title");
            document.routeDocument("");
            document = new WorkflowDocument(nid, docType);
            document.setTitle("a title");
            document.routeDocument("");
        }

        return 5 * docTypes.length;
    }

    /**
     * tests that the standard actionlistemailserviceimpl and refactored hardcodedemailcontentservice
     * produce the same email messages
     * @throws Exception
     */
    @Test
    public void testGenerateReminders() throws Exception {
        WorkflowUser wfuser = KEWServiceLocator.getUserService().getWorkflowUser(new AuthenticationUserId("arh14"));
        int count = generateDocs(new String[] { "PingDocument", "PingDocumentWithEmailAttrib" }, wfuser);
        testEmailContentGeneration(wfuser, count);
    }

    /**
     * tests custom stylesheet
     * @throws Exception
     */
    @Test
    public void testGenerateRemindersCustomStyleSheet() throws Exception {
        loadXmlFile("customEmailStyleData.xml");
        assertNotNull(KEWServiceLocator.getStyleService().getStyle("kew.email.style"));
        WorkflowUser user = KEWServiceLocator.getUserService().getWorkflowUser(new AuthenticationUserId("arh14"));
        int count = generateDocs(new String[] { "PingDocument", "PingDocumentWithEmailAttrib" }, user);

        Collection<ActionItem> actionItems = KEWServiceLocator.getActionListService().getActionList(user, null);
        assertEquals("user should have " + count + " items in his action list.", count, actionItems.size());

        EmailContent content = styleableContentService.generateImmediateReminder(user, actionItems.iterator().next(), KEWServiceLocator.getDocumentTypeService().findByName(actionItems.iterator().next().getDocName()));
        assertTrue("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("CUSTOM:"));

        content = styleableContentService.generateDailyReminder(user, actionItems);
        assertTrue("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("CUSTOM:"));

        content = styleableContentService.generateWeeklyReminder(user, actionItems);
        assertTrue("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("CUSTOM:"));
    }

    /**
     * tests custom stylesheet
     * @throws Exception
     */
    @Test
    public void testGenerateRemindersDocCustomStyleSheet() throws Exception {
        // we need to make sure that the immediate email message is customized on a per-doc basis
        // so we need to easily distinguish from the global style and the custom style
        // an easy way to do that is use two styles that have introduced obvious and blatent
        // distinguishing marker...so we just reuse the global custom email style here
        loadXmlFile("customEmailStyleData.xml");
        loadXmlFile("docCustomEmailStyleData.xml");
        assertNotNull(KEWServiceLocator.getStyleService().getStyle("kew.email.style"));
        assertNotNull(KEWServiceLocator.getStyleService().getStyle("doc.custom.email.style"));

        WorkflowUser user = KEWServiceLocator.getUserService().getWorkflowUser(new AuthenticationUserId("arh14"));
        int count = generateDocs(new String[] { "PingDocumentCustomStyle" }, user);

        Collection<ActionItem> actionItems = KEWServiceLocator.getActionListService().getActionList(user, null);
        assertEquals("user should have " + count + " items in his action list.", count, actionItems.size());

        EmailContent content = styleableContentService.generateImmediateReminder(user, actionItems.iterator().next(), KEWServiceLocator.getDocumentTypeService().findByName(actionItems.iterator().next().getDocName()));
        // immediate email reminder should have used the doc type email style and NOT the global style
        assertFalse("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertFalse("Unexpected body", content.getBody().startsWith("CUSTOM:"));
        assertTrue("Unexpected subject", content.getSubject().startsWith("DOCTYPE CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("DOCTYPE CUSTOM:"));


        // daily and weekly are unchanged since they are not document type specific
        content = styleableContentService.generateDailyReminder(user, actionItems);
        assertTrue("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("CUSTOM:"));

        content = styleableContentService.generateWeeklyReminder(user, actionItems);
        assertTrue("Unexpected subject", content.getSubject().startsWith("CUSTOM:"));
        assertTrue("Unexpected body", content.getBody().startsWith("CUSTOM:"));
    }

    /**
     * tests loading a custom stylesheet that has entities that causes XPath to get confused down the ingestion pipeline...
     * @throws Exception
     */
    @Test
    public void testBadCustomStyleSheet() throws Exception {
        loadXmlFile("badCustomEmailStyleData.xml");
        // this doesn't get loaded
        assertNull(KEWServiceLocator.getStyleService().getStyle("bad.kew.email.style"));
    }

    /**
     * tests feedback email content generation
     * @throws Exception
     */
    @Test
    public void testFeedback() throws Exception {
        HardCodedEmailContentServiceImpl hcContentService = new HardCodedEmailContentServiceImpl();
        hcContentService.setDeploymentEnvironment("dev");

        StyleableEmailContentServiceImpl styleContentService = new StyleableEmailContentServiceImpl();
        styleContentService.setDeploymentEnvironment("dev");
        styleContentService.setStyleService(KEWServiceLocator.getStyleService());

        FeedbackForm form = new FeedbackForm();
        form.setComments("this is \r\n a few lines of \r\n comments");
        form.setDocumentType("PingDocument");
        form.setEdenCategory("an eden category");
        form.setException(new Exception().toString());
        form.setFirstName("first name");
        form.setLastName("last name");
        form.setNetworkId("network id");
        form.setPageUrl("page url");
        form.setPhone("555-555-5555");
        form.setTimeDate(new Date().toString());
        form.setUserEmail("user@unittest");
        form.setUserName("user name");
        // don't set route header id at this point

        EmailContent hcContent = hcContentService.generateFeedback(form);
        EmailContent sContent = styleContentService.generateFeedback(form);
        assertEquals(hcContent.getSubject().replace("\\s+", " "), sContent.getSubject().replace("\\s+", " "));
        assertEquals(hcContent.getBody().replace("\\s+", " "), sContent.getBody().replace("\\s+", " "));
        assertEquals(hcContent.isHtml(), sContent.isHtml());

        // now test with the route header id set
        form.setRouteHeaderId("12345");

        hcContent = hcContentService.generateFeedback(form);
        sContent = styleContentService.generateFeedback(form);
        assertEquals(hcContent.getSubject().replace("\\s+", " "), sContent.getSubject().replace("\\s+", " "));
        assertEquals(hcContent.getBody().replace("\\s+", " "), sContent.getBody().replace("\\s+", " "));
        assertEquals(hcContent.isHtml(), sContent.isHtml());
    }
}