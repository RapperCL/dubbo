/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.compact.Dubbo2CompactUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;

import java.lang.annotation.Annotation;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.apache.dubbo.common.utils.AnnotationUtils.getAttribute;
import static org.apache.dubbo.common.utils.ArrayUtils.isNotEmpty;
import static org.apache.dubbo.common.utils.ClassUtils.isGenericClass;
import static org.apache.dubbo.common.utils.ClassUtils.resolveClass;
import static org.apache.dubbo.common.utils.StringUtils.isEmpty;

/**
 * The resolver class for {@link Service @Service}
 *
 * @see Service
 * @see com.alibaba.dubbo.config.annotation.Service
 * @since 2.7.6
 */
public class ServiceAnnotationResolver {

    /**
     * The annotation {@link Class classes} of Dubbo Service (read-only)
     *
     * @since 2.7.9
     */
    public static List<Class<? extends Annotation>> SERVICE_ANNOTATION_CLASSES = loadServiceAnnotationClasses();

    private static List<Class<? extends Annotation>> loadServiceAnnotationClasses() {
        if (Dubbo2CompactUtils.isEnabled() && Dubbo2CompactUtils.isServiceClassLoaded()) {
            return unmodifiableList(asList(DubboService.class, Service.class, Dubbo2CompactUtils.getServiceClass()));
        } else {
            return unmodifiableList(asList(DubboService.class, Service.class));
        }
    }

    private final Annotation serviceAnnotation;

    private final Class<?> serviceType;

    public ServiceAnnotationResolver(Class<?> serviceType) throws IllegalArgumentException {
        this.serviceType = serviceType;
        this.serviceAnnotation = getServiceAnnotation(serviceType);
    }

    private Annotation getServiceAnnotation(Class<?> serviceType) {

        Annotation serviceAnnotation = null;

        for (Class<? extends Annotation> serviceAnnotationClass : SERVICE_ANNOTATION_CLASSES) {
            serviceAnnotation = serviceType.getAnnotation(serviceAnnotationClass);
            if (serviceAnnotation != null) {
                break;
            }
        }

        if (serviceAnnotation == null) {
            throw new IllegalArgumentException(format("Any annotation of [%s] can't be annotated in the service type[%s].",
                    SERVICE_ANNOTATION_CLASSES,
                    serviceType.getName()
            ));
        }

        return serviceAnnotation;
    }

    /**
     * Resolve the class name of interface
     *
     * @return if not found, return <code>null</code>
     */
    public String resolveInterfaceClassName() {

        Class interfaceClass;
        // first, try to get the value from "interfaceName" attribute
        String interfaceName = resolveAttribute("interfaceName");

        if (isEmpty(interfaceName)) { // If not found, try "interfaceClass"
            interfaceClass = resolveAttribute("interfaceClass");
        } else {
            interfaceClass = resolveClass(interfaceName, getClass().getClassLoader());
        }

        if (isGenericClass(interfaceClass)) {
            interfaceName = interfaceClass.getName();
        } else {
            interfaceName = null;
        }

        if (isEmpty(interfaceName)) { // If not fund, try to get the first interface from the service type
            Class[] interfaces = serviceType.getInterfaces();
            if (isNotEmpty(interfaces)) {
                interfaceName = interfaces[0].getName();
            }
        }

        return interfaceName;
    }

    public String resolveVersion() {
        return resolveAttribute("version");
    }

    public String resolveGroup() {
        return resolveAttribute("group");
    }

    private <T> T resolveAttribute(String attributeName) {
        return getAttribute(serviceAnnotation, attributeName);
    }

    public Annotation getServiceAnnotation() {
        return serviceAnnotation;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }
}
