/*
 * Copyright 2005-2007 The Kuali Foundation
 *
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
package org.kuali.rice.kew.batch;

import org.apache.log4j.Logger;
import org.kuali.rice.kew.service.KEWServiceLocator;
import org.kuali.rice.kew.util.Utilities;
import org.kuali.rice.kew.xml.ClassLoaderEntityResolver;
import org.kuali.rice.kew.xml.XmlLoader;
import org.springframework.beans.factory.BeanInitializationException;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;


/**
 * XmlIngesterService implementation which delegates to XmlDigesterService.
 * This implementation goes through some pains to ensure that the types of
 * xml doc (determined by file name convention) are issued to the XmlDigesterService
 * in a pre-ordained order in an effort to avoid dependency problems.  This implementation
 * is not responsible for knowing about the mappings between types and services, but
 * only the ordering of types, for the moment.
 * NOTE: when types are merged into a universal document, we need to decide how to handle
 * rollback if any specific type <i>in</i> that document fails, given that the current,
 * legacy implementation assumes that a given XmlDoc consists of one and only one type
 * and as such can be rolled back atomically.  For instance, if universal doc now contains
 * types A, B, and C, and it invokes ServiceA, ServiceB, and ServiceC in succession on the
 * entire document, and ServiceB throws an exception attempting to parse B content...
 * is it sufficient to rollback only that entry, or do we rollback the whole document
 * and consider it "tainted"? (not to mention whether we should roll back the entire collection
 * of which the document is a part - for now we do NOT rollback a collection or workflow data doc,
 * but it is merely moved to a "problem" directory by the poller.  the implementation does not yet
 * specifically note which document or type (and potentially eventually which entry) failed in the
 * collection or workflow data doc)
 *
 * NOTE: this service must be invoked only after all other services have initialized
 * this <i>should</i> be the case since the LifeCycle is kicked off after contextInitialized,
 * which <i>should</i> occur after Spring is actually done initializing.  But is it, considering
 * we are asynchronously initializing Spring?  There is a 30 second built-in delay before
 * XmlPoller is first run, but suffice it to say there is a possible race condition.
 *
 * @see org.kuali.rice.kew.batch.XmlIngesterService
 * @see org.kuali.rice.kew.batch.XmlDigesterServiceImpl
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class XmlIngesterServiceImpl implements XmlIngesterService {
    private static final Logger LOG = Logger.getLogger(XmlIngesterServiceImpl.class);

    /**
     * The entity resolver to use during validation
     */
    private EntityResolver resolver = new ClassLoaderEntityResolver();

    private XmlDigesterService digesterService;

    /**
     * Whether to validate at all
     */
    private boolean validate = true;

    /**
     * A list of service beans implementing XmlLoader, in the order of precedence
     * in which we should load xml doc content types.  It is implicit that any
     * unspecified XmlLoader beans will be invoked after all ordered beans.  In
     * the future this may be configured through the use of a special marker (such as null)
     * to indicate the "all others" set, but for now everything that is not specified
     * just gets invoked last (in arbitrary order)
     */
    private List<String> serviceOrder;

    // ---- bean properties

    public void setXmlDigesterService(XmlDigesterService digesterService) {
        this.digesterService = digesterService;
    }

    public void setEntityResolver(EntityResolver resolver) {
        this.resolver = resolver;
    }

    public void setServiceOrder(List<String> serviceOrder) throws BeanInitializationException {
        //eat Strings for now
//        Iterator orderIt = serviceOrder.iterator();
//        while (orderIt.hasNext()) {
//            Object o = orderIt.next();
//            if (!(o instanceof XmlLoader)) {
//                String message = o + " does not implement XmlLoader.  Only XmlLoader beans can be specified in the service order!";
//                LOG.error(message);
//                throw new BeanInitializationException(message);
//            }
//        }
        this.serviceOrder = serviceOrder;
    }

    public void setValidate(boolean b) {
        validate = b;
    }

    // ---- implementation

    private static void addProcessingException(XmlDoc xmlDoc, String message, Throwable t) {
        String msg = xmlDoc.getProcessingMessage();
        if (msg == null) {
            msg = "";
        }
        msg += message + "\n" + Utilities.collectStackTrace(t);
        xmlDoc.setProcessingMessage(msg);
    }

    private static void validate(final XmlDoc xmlDoc, EntityResolver resolver) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        dbf.setNamespaceAware( true );
        dbf.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(resolver);
        db.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException se) {
                LOG.warn("Warning parsing xml doc " + xmlDoc, se);
                addProcessingException(xmlDoc, "Warning parsing xml doc " + xmlDoc, se);
            }
            public void error(SAXParseException se) throws SAXException {
                LOG.error("Error parsing xml doc " + xmlDoc, se);
                addProcessingException(xmlDoc, "Error parsing xml doc " + xmlDoc, se);
                throw se;
            }
            public void fatalError(SAXParseException se) throws SAXException {
                LOG.error("Fatal error parsing xml doc " + xmlDoc, se);
                addProcessingException(xmlDoc, "Fatal error parsing xml doc " + xmlDoc, se);
                throw se;
            }
        });
        db.parse(xmlDoc.getStream());
    }

    /**
     * Validates (if possible) all XmlDocs, and accumulates only those
     * which either were not possible to validate, or passed validation.
     * @param collections collection of XmlDocCollection
     * @param resolver the entity resolver to use
     * @param successful xmldoccollections in which all docs successfully validated
     * @param failed xmldoccollections in which one or more docs failed validation
     */
    private static void validate(List<XmlDocCollection> collections, EntityResolver resolver, Set<XmlDocCollection> successful, Set<XmlDocCollection> failed) {
        // for every collection, validate all docs
        for (XmlDocCollection collection : collections)
        {

            // for every xml doc in the collection, try to validate it
            for (XmlDoc xmlDoc : collection.getXmlDocs())
            {
                try
                {
                    validate(xmlDoc, resolver);
                } catch (Exception e)
                {
                    LOG.error("Error validating doc: " + xmlDoc, e);
                    addProcessingException(xmlDoc, "Error validating doc: " + xmlDoc, e);
                    // validation failed, so add collection to successful set
                    // do not break here, so that we can attempt validation on all
                    // docs in a collection; since validation has no side-effects
                    // we might as well validate all the docs now instead of forcing
                    // the user to continually re-submit
                    failed.add(collection);
                }
            }

            // all files validated, so add collection to successful set
            successful.add(collection);
        }
    }

    private void ingest(XmlLoader xmlLoader, Collection<XmlDocCollection> xmlDocCollections, String principalId, Set<Object> successful, Set<XmlDocCollection> failed) {
        for (XmlDocCollection xmlDocCollection : xmlDocCollections)
        {

            if (failed.contains(xmlDocCollection))
            {
                LOG.debug("Skipping " + xmlDocCollection.getFile() + "...");
                continue;
            }

            try
            {
                //SpringServiceLocator.getXmlDigesterService().digest(xmlLoader, xmlDocCollection, user);
                digesterService.digest(xmlLoader, xmlDocCollection, principalId);
            } catch (Exception e)
            {
                LOG.error("Caught Exception loading xml data from " + xmlDocCollection.getFile() + ".  Will move associated file to problem dir.", e);
                failed.add(xmlDocCollection);
            }
        }
    }

    public Collection<XmlDocCollection> ingest(List<XmlDocCollection> collections) throws Exception {
        return ingest(collections, null);
    }

    private void ingestThroughOrderedLoaders(Collection<XmlDocCollection> xmlDocCollections, String principalId, Set<Object> successful, Set<XmlDocCollection> failed) {
        LOG.debug("Ingesting through ordered XmlLoaders");
        for (String aServiceOrder : serviceOrder)
        {
            XmlLoader xmlLoader = (XmlLoader) KEWServiceLocator.getService(aServiceOrder);
            LOG.debug("Ingesting through ordered XmlLoader: " + xmlLoader);
            ingest(xmlLoader, xmlDocCollections, principalId, successful, failed);
        }
    }

    /* FIXME commented this out because its causing problems with the default User and Workgroup services
	private void ingestThroughUnorderedLoaders(Collection xmlDocCollections, WorkflowUser user, Set successful, Set failed) {
		Map beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, XmlLoader.class);

        // ingest docs by remaining (if any) unordered services
        LOG.debug("Ingesting through un-ordered XmlLoaders");
        Iterator entryIt = beans.entrySet().iterator();
        while (entryIt.hasNext()) {
            Map.Entry entry = (Map.Entry) entryIt.next();
            XmlLoader xmlLoader = (XmlLoader) entry.getValue();
            if (serviceOrder.contains(entry.getKey())) {
                LOG.debug("Skipping ordered XmlLoader: " + entry.getKey() + " " + xmlLoader.getClass());
                continue;
            }
            LOG.debug("Ingesting through un-ordered XmlLoader: " + entry.getKey() + " " + xmlLoader.getClass());
            ingest(xmlLoader, xmlDocs, user);
        }
    }*/

    public Collection<XmlDocCollection> ingest(List<XmlDocCollection> collections, String principalId) {
        Set<XmlDocCollection> failed = new LinkedHashSet<XmlDocCollection>();
        // validate all the docs up-front because we will be iterating over them
        // multiple times: one for each XmlLoader.  If we delegated validation to
        // XmlDigesterService then the docs would re-validated over and over again,
        // for each XmlLoader
        if (validate) {
            Set<XmlDocCollection> successful = new LinkedHashSet<XmlDocCollection>();
            validate(collections, resolver, successful, failed);
            collections = new LinkedList<XmlDocCollection>(successful);
        }

        Set<Object> successful = new LinkedHashSet<Object>();
        // ingest docs first by ordered services
        ingestThroughOrderedLoaders(collections, principalId, successful, failed);
        // then by unordered services
//        collections = new LinkedList(successful);

        //ingestThroughUnorderedLoaders(collections, user, successful, failed);

        return failed;
    }
}