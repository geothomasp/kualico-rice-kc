/**
 * Copyright 2005-2013 The Kuali Foundation
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
package org.kuali.rice.krad.data.jpa;

import com.google.common.collect.Sets;
import org.kuali.rice.core.api.config.property.ConfigContext;
import org.kuali.rice.core.api.criteria.LookupCustomizer;
import org.kuali.rice.core.api.criteria.QueryByCriteria;
import org.kuali.rice.core.api.criteria.QueryResults;
import org.kuali.rice.krad.data.CompoundKey;
import org.kuali.rice.krad.data.DataObjectService;
import org.kuali.rice.krad.data.PersistenceOption;
import org.kuali.rice.krad.data.config.ConfigConstants;
import org.kuali.rice.krad.data.metadata.DataObjectMetadata;
import org.kuali.rice.krad.data.provider.PersistenceProvider;
import org.kuali.rice.krad.data.provider.util.ReferenceLinker;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.metamodel.ManagedType;
import java.util.Set;

/**
 * JPA PersistenceProvider impl
 */
@Transactional
public class JpaPersistenceProvider implements PersistenceProvider, InitializingBean {

    private EntityManager sharedEntityManager;
    private DataObjectService dataObjectService;
    private ReferenceLinker referenceLinker;

    /**
     * Initialization-on-demand holder idiom for thread-safe lazy loading of configuration.
     */
    private static final class LazyConfigHolder {
        private static final boolean autoFlush = ConfigContext.getCurrentContextConfig().getBooleanProperty(ConfigConstants.JPA_AUTO_FLUSH, false);
    }

    public EntityManager getSharedEntityManager() {
        return sharedEntityManager;
    }

    public void setSharedEntityManager(EntityManager sharedEntityManager) {
        this.sharedEntityManager = sharedEntityManager;
    }

    @Required
    public void setDataObjectService(DataObjectService dataObjectService) {
        this.dataObjectService = dataObjectService;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.referenceLinker = new ReferenceLinker(dataObjectService);
    }

    @Override
    public <T> T save(T dataObject, PersistenceOption... options) {
        verifyDataObjectWritable(dataObject);

		Set<PersistenceOption> optionSet = Sets.newHashSet(options);

		dataObject = sharedEntityManager.merge(dataObject);

        if (optionSet.contains(PersistenceOption.LINK)) {
            referenceLinker.linkObjects(dataObject);
        }

        if(optionSet.contains(PersistenceOption.FLUSH) || LazyConfigHolder.autoFlush){
			sharedEntityManager.flush();
        }

		return dataObject;
    }

    @Override
    public <T> T find(Class<T> type, Object id) {
        if (id instanceof CompoundKey) {
			QueryResults<T> results = findMatching(type,
					QueryByCriteria.Builder.andAttributes(((CompoundKey) id).getKeys()).build());
			if (results.getResults().size() > 1) {
				throw new NonUniqueResultException("Error Compound Key: " + id + " on class " + type.getName()
						+ " returned more than one row.");
			}
            if (!results.getResults().isEmpty()) {
				return results.getResults().get(0);
            }
			return null;
        } else {
            return sharedEntityManager.find(type, id);
        }
    }

    @Override
    public <T> QueryResults<T> findMatching(Class<T> type, QueryByCriteria queryByCriteria) {
        return new JpaCriteriaQuery(sharedEntityManager).lookup(type, queryByCriteria);
    }

    @Override
    public <T> QueryResults<T> findMatching(Class<T> type, QueryByCriteria queryByCriteria, LookupCustomizer<T> lookupCustomizer) {
        return new JpaCriteriaQuery(sharedEntityManager).lookup(type, queryByCriteria, lookupCustomizer);
    }

    @Override
    public void delete(Object dataObject) {
        verifyDataObjectWritable(dataObject);
        sharedEntityManager.remove(sharedEntityManager.merge(dataObject));
    }

    @Override
    public boolean handles(Class<?> type) {
        try {
            ManagedType<?> managedType = sharedEntityManager.getMetamodel().managedType(type);
            return managedType != null;
        } catch (IllegalArgumentException iae) {
            return false;
        }
    }

    @Override
    public void flush(Class<?> type){
        sharedEntityManager.flush();
    }

    protected void verifyDataObjectWritable(Object dataObject) {
        DataObjectMetadata metaData = dataObjectService.getMetadataRepository().getMetadata(dataObject.getClass());
        if (metaData == null) {
            throw new IllegalArgumentException("Given data object class is not loaded into the MetadataRepository: " + dataObject.getClass());
        }
        if (metaData.isReadOnly()) {
            throw new UnsupportedOperationException(dataObject.getClass() + " is read-only");
        }
    }

}