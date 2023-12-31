/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.transform.description;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * Resource specific transformation description builder. This is a convenience API over the
 * {@linkplain org.jboss.as.controller.transform.TransformersSubRegistration} and can be used to add common policies
 * when registering resource or operation transformers.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface ResourceTransformationDescriptionBuilder extends TransformationDescriptionBuilder {

    /**
     * Get a builder to define custom attribute transformation rules.
     * These rules transform the resource and its operations.
     *
     * @return the attribute transformation builder
     */
    AttributeTransformationDescriptionBuilder getAttributeBuilder();

    /**
     * Add an operation transformation entry for a given operation. By default all operations inherit the attribute
     * transformation rules from this transformation description. This behavior can be overridden for a given operation.
     *
     * @param operationName the operation name
     * @return the operation transformation builder
     */
    OperationTransformationOverrideBuilder addOperationTransformationOverride(String operationName);

    /**
     * Add an operation transformer. Unlike the the {@linkplain #addOperationTransformationOverride(String)} this will
     * use the {@linkplain OperationTransformer} without adding any additional capabilities.
     *
     * @param operationName the operation name
     * @param operationTransformer the operation transformer
     * @return the builder for the current instance
     */
    ResourceTransformationDescriptionBuilder addRawOperationTransformationOverride(String operationName, OperationTransformer operationTransformer);

    /**
     * Set an <b>optional</b> custom resource transformer. This transformer is going to be called after all attribute transformations
     * set up by {@link #getAttributeBuilder()} and needs to take care of adding the currently transformed resource properly. If not specified,
     * the resource will be added according to other rules defined by this builder.
     *
     * @param resourceTransformer the resource transformer
     * @return the builder for the current resource
     */
    ResourceTransformationDescriptionBuilder setCustomResourceTransformer(ResourceTransformer resourceTransformer);

    /**
     * Add a child resource to this builder. This is going to register the child automatically at the
     * {@linkplain org.jboss.as.controller.transform.TransformersSubRegistration} when registering the transformation
     * description created by this builder.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(PathElement pathElement);

    /**
     * Add a child resource to this builder. This is going to register the child automatically at the
     * {@linkplain org.jboss.as.controller.transform.TransformersSubRegistration} when registering the transformation
     * description created by this builder.
     *
     * @param pathElement the path element
     * @param dynamicDiscardPolicy a checker to decide whether the child should be added or not
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(PathElement pathElement, DynamicDiscardPolicy dynamicDiscardPolicy);

    /**
     * Add a child resource to this builder. This is going to register the child automatically at the
     * {@linkplain org.jboss.as.controller.transform.TransformersSubRegistration} when registering the transformation
     * description created by this builder.
     *
     * @param definition the resource definition
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(ResourceDefinition definition);

    /**
     * Add a child resource to this builder. This is going to register the child automatically at the
     * {@linkplain org.jboss.as.controller.transform.TransformersSubRegistration} when registering the transformation
     * description created by this builder.
     *
     * @param definition the resource definition
     * @param dynamicDiscardPolicy a checker to decide whether the child should be added or not
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildResource(ResourceDefinition definition, DynamicDiscardPolicy dynamicDiscardPolicy);


    /**
     * Recursively discards all child resources and its operations.
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    DiscardTransformationDescriptionBuilder discardChildResource(PathElement pathElement);

    /**
     * Recursively rejects all child resources and its operations
     *
     * @param pathElement the path element
     * @return the builder for the child resource
     */
    RejectTransformationDescriptionBuilder rejectChildResource(PathElement pathElement);

    /**
     * Add a child resource, where all operations will get redirected to the legacy address. You can either pass in
     * <ul>
     * <li><b>Fixed elements</b> - e.g. {@code current:addr1=test} + {@code legacy:addr2=toast}, in which case {@code addr1=test} gets redirected to {@code addr2=toast}}</li>
     * <li><b>Wildcard elements</b> - e.g. {@code current:addr1=*} + {@code legacy:addr2=*}, in which case {@code addr1=test} gets redirected to {@code addr2=test},
     * {@code addr1=ping} gets redirected to {@code addr2=ping}, etc.</li>
     * </ul>
     *
     * @param current the current path element
     * @param legacy the legacy path element.
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement current, PathElement legacy);

    /**
     * Add a child resource, where all operations will get redirected to the legacy address. You can either pass in
     * <ul>
     * <li><b>Fixed elements</b> - e.g. {@code current:addr1=test} + {@code legacy:addr2=toast}, in which case {@code addr1=test} gets redirected to {@code addr2=toast}}</li>
     * <li><b>Wildcard elements</b> - e.g. {@code current:addr1=*} + {@code legacy:addr2=*}, in which case {@code addr1=test} gets redirected to {@code addr2=test},
     * {@code addr1=ping} gets redirected to {@code addr2=ping}, etc.</li>
     * </ul>
     *
     * @param current the current path element
     * @param legacy the legacy path element.
     * @param dynamicDiscardPolicy a checker to decide whether the child should be added or not
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement current, PathElement legacy, DynamicDiscardPolicy dynamicDiscardPolicy);

    /**
     * Add a child resource, where all operation will get redirected to a different address defined by
     * the path transformation.
     *
     * @param pathElement the path element of the child
     * @param pathAddressTransformer the path transformation
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement pathElement, PathAddressTransformer pathAddressTransformer);

    /**
     * Add a child resource, where all operation will get redirected to a different address defined by
     * the path transformation.
     *
     * @param pathElement the path element of the child
     * @param pathAddressTransformer the path transformation
     * @param dynamicDiscardPolicy a checker to decide whether the child should be added or not
     * @return the builder for the child resource
     */
    ResourceTransformationDescriptionBuilder addChildRedirection(PathElement pathElement, PathAddressTransformer pathAddressTransformer, DynamicDiscardPolicy dynamicDiscardPolicy);

    /**
     * Add an already created {@link TransformationDescriptionBuilder} as a child of this builder.
     *
     * @param builder the builder
     * @return the builder for this resource
     */
    ResourceTransformationDescriptionBuilder addChildBuilder(TransformationDescriptionBuilder builder);

    /**
     * Don't forward and just discard the operation.
     *
     * @param operationNames the operation names
     * @return the builder for this resource
     */
    ResourceTransformationDescriptionBuilder discardOperations(String... operationNames);

}
