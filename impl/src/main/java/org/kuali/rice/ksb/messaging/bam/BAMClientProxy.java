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
package org.kuali.rice.ksb.messaging.bam;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.kuali.rice.core.proxy.BaseTargetedInvocationHandler;
import org.kuali.rice.core.resourceloader.ContextClassLoaderProxy;
import org.kuali.rice.core.util.ClassLoaderUtils;
import org.kuali.rice.core.util.ExceptionUtils;
import org.kuali.rice.ksb.messaging.ServiceInfo;
import org.kuali.rice.ksb.messaging.bam.service.BAMService;
import org.kuali.rice.ksb.service.KSBServiceLocator;


/**
 * A client-side proxy for that records an entry in the BAM for invocations
 * on the proxied service.
 *
 * @see BAMService
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
public class BAMClientProxy extends BaseTargetedInvocationHandler {

	private ServiceInfo serviceInfo;
	
	private BAMClientProxy(Object target, ServiceInfo serviceInfo) {
		super(target);
		this.serviceInfo = serviceInfo;
	}
	
	public static Object wrap(Object target, ServiceInfo serviceInfo) {
		return Proxy.newProxyInstance(ClassLoaderUtils.getDefaultClassLoader(), ContextClassLoaderProxy.getInterfacesToProxyIncludeSpring(target), new BAMClientProxy(target, serviceInfo)); 
	}
	
	protected Object invokeInternal(Object proxyObject, Method method, Object[] arguments) throws Throwable {
		BAMTargetEntry bamTargetEntry = KSBServiceLocator.getBAMService().recordClientInvocation(this.serviceInfo, getTarget(), method, arguments);
		try {
			return method.invoke(getTarget(), arguments);	
		} catch (Throwable throwable) {
			throwable = ExceptionUtils.unwrapActualCause(throwable);
			KSBServiceLocator.getBAMService().recordClientInvocationError(throwable, bamTargetEntry);
			throw throwable;
		}
	}
}