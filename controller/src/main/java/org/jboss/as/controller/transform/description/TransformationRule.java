/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.controller.transform.description;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformationTarget;
import org.jboss.as.controller.transform.TransformersLogger;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emanuel Muckenhuber
 */
abstract class TransformationRule {

    abstract void transformOperation(ModelNode operation, PathAddress address, ChainedOperationContext context) throws OperationFailedException;
    abstract void transformResource(Resource resource, PathAddress address, ChainedResourceContext context) throws OperationFailedException;

    static ModelNode cloneAndProtect(ModelNode modelNode) {
        ModelNode clone = modelNode.clone();
        clone.protect();
        return clone;
    }

    abstract static class AbstractChainedContext {

        private final TransformationContextWrapper context;
        protected AbstractChainedContext(final TransformationContext context) {
            this.context = new TransformationContextWrapper(context);
        }

        protected TransformationContext getContext() {
            return context;
        }

        void setImmutableResource(boolean immutable) {
            context.setImmutableResource(immutable);
        }
    }

    abstract static class ChainedOperationContext extends AbstractChainedContext {

        private final List<OperationTransformer.TransformedOperation> transformed = new ArrayList<OperationTransformer.TransformedOperation>();
        private ModelNode lastOperation;
        protected ChainedOperationContext(TransformationContext context) {
            super(context);
        }

        protected void recordTransformedOperation(OperationTransformer.TransformedOperation operation) {
            lastOperation = operation.getTransformedOperation();
            transformed.add(operation);
        }

        void invokeNext(ModelNode operation) throws OperationFailedException {
            invokeNext(new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT));
        }

        abstract void invokeNext(OperationTransformer.TransformedOperation transformedOperation) throws OperationFailedException;

        protected OperationTransformer.TransformedOperation createOp() {
            if(transformed.size() == 1) {
                return transformed.get(0);
            }
            return new ChainedTransformedOperation(lastOperation, transformed);
        }

    }

    abstract static class ChainedResourceContext extends AbstractChainedContext {

        protected ChainedResourceContext(ResourceTransformationContext context) {
            super(context);
        }

        abstract void invokeNext(Resource resource) throws OperationFailedException;

    }

    private static class TransformationContextWrapper implements TransformationContext {

        private final TransformationContext delegate;
        private volatile boolean immutable;

        private TransformationContextWrapper(TransformationContext delegate) {
            this.delegate = delegate;
        }

        @Override
        public TransformationTarget getTarget() {
            return delegate.getTarget();
        }

        @Override
        public ProcessType getProcessType() {
            return delegate.getProcessType();
        }

        @Override
        public RunningMode getRunningMode() {
            return delegate.getRunningMode();
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistration(PathAddress address) {
            return delegate.getResourceRegistration(address);
        }

        @Override
        public ImmutableManagementResourceRegistration getResourceRegistrationFromRoot(PathAddress address) {
            return delegate.getResourceRegistrationFromRoot(address);
        }

        @Override
        public Resource readResource(PathAddress address) {
            Resource resource = delegate.readResource(address);
            if (resource != null) {
                return immutable ? new ProtectedModelResource<Resource>(resource) : resource;
            }
            return null;
        }

        @Override
        public Resource readResourceFromRoot(PathAddress address) {
            Resource resource = delegate.readResourceFromRoot(address);
            if (resource != null) {
                return immutable ? new ProtectedModelResource<Resource>(resource) : resource;
            }
            return null;
        }

        @Override
        public TransformersLogger getLogger() {
            return delegate.getLogger();
        }

        void setImmutableResource(boolean immutable) {
            this.immutable = immutable;
        }

        @Override
        public <T> T getAttachment(OperationContext.AttachmentKey<T> key) {
            return delegate.getAttachment(key);
        }

        @Override
        public <T> T attach(OperationContext.AttachmentKey<T> key, T value) {
            return delegate.attach(key, value);
        }

        @Override
        public <T> T attachIfAbsent(OperationContext.AttachmentKey<T> key, T value) {
            return delegate.attachIfAbsent(key, value);
        }

        @Override
        public <T> T detach(OperationContext.AttachmentKey<T> key) {
            return delegate.detach(key);
        }

    }

    private static class ChainedTransformedOperation extends OperationTransformer.TransformedOperation {

        private final List<OperationTransformer.TransformedOperation> delegates;
        private volatile String failure;
        private volatile boolean initialized;

        public ChainedTransformedOperation(final ModelNode transformedOperation, final List<OperationTransformer.TransformedOperation> delegates) {
            super(transformedOperation, null);
            this.delegates = delegates;
        }

        @Override
        public ModelNode getTransformedOperation() {
            return super.getTransformedOperation();
        }

        @Override
        public OperationResultTransformer getResultTransformer() {
            return this;
        }

        @Override
        public boolean rejectOperation(ModelNode preparedResult) {
            for (OperationTransformer.TransformedOperation delegate : delegates) {
                if (delegate.rejectOperation(preparedResult)) {
                    failure = delegate.getFailureDescription();
                    initialized = true; //See comment in getFailureDescription()
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getFailureDescription() {
            //In real life this will always be initialized by the transforming proxy before anyone calls this method
            //For testing we call it directly from ModelTestUtils
            if (!initialized) {
                for (TransformedOperation delegate : delegates) {
                    String failure = delegate.getFailureDescription();
                    if (failure != null) {
                        return failure;
                    }
                }
            }
            return failure;
        }

        @Override
        public ModelNode transformResult(ModelNode result) {
            ModelNode currentResult = result;
            final int size = delegates.size();
            for (int i = size - 1 ; i >= 0 ; --i) {
                currentResult = delegates.get(i).transformResult(currentResult);
            }
            return currentResult;
        }
    }

   /**
    *  Implementation of resource that returns an unmodifiable model
    */
   private static class ProtectedModelResource<T extends Resource> implements Resource {

       protected T delegate;

       ProtectedModelResource(T delegate){
           this.delegate = delegate;
       }

       @Override
       public ModelNode getModel() {
           return TransformationRule.cloneAndProtect(delegate.getModel());
       }

       @Override
       public void writeModel(ModelNode newModel) {
           throw ControllerLogger.ROOT_LOGGER.immutableResource();
       }

       @Override
       public boolean isModelDefined() {
           return delegate.isModelDefined();
       }

       @Override
       public boolean hasChild(PathElement element) {
           return delegate.hasChild(element);
       }

       @Override
       public Resource getChild(PathElement element) {
           Resource resource = delegate.getChild(element);
           if (resource != null) {
               return new ProtectedModelResource<Resource>(resource);
           }
           return null;
       }

       @Override
       public Resource requireChild(PathElement element) {
           Resource resource = delegate.requireChild(element);
           if (resource != null) {
               return new ProtectedModelResource<Resource>(resource);
           }
           return null;
       }

       @Override
       public boolean hasChildren(String childType) {
           return delegate.hasChildren(childType);
       }

       @Override
       public Resource navigate(PathAddress address) {
           Resource resource = delegate.navigate(address);
           if (resource != null) {
               return new ProtectedModelResource<Resource>(resource);
           }
           return null;
       }

       @Override
       public Set<String> getChildTypes() {
           return delegate.getChildTypes();
       }

       @Override
       public Set<String> getChildrenNames(String childType) {
           return delegate.getChildrenNames(childType);
       }

       @Override
       public Set<ResourceEntry> getChildren(String childType) {
           Set<ResourceEntry> children = delegate.getChildren(childType);
           if (children != null) {
               Set<ResourceEntry> protectedChildren = new LinkedHashSet<Resource.ResourceEntry>();
               for (ResourceEntry entry : children) {
                   protectedChildren.add(new ProtectedModelResourceEntry(entry));
               }
               return protectedChildren;
           }
           return null;
       }

       @Override
       public void registerChild(PathElement address, Resource resource) {
           throw ControllerLogger.ROOT_LOGGER.immutableResource();
       }

       @Override
       public void registerChild(PathElement address, int index, Resource resource) {
           throw ControllerLogger.ROOT_LOGGER.immutableResource();
       }

       @Override
       public Resource removeChild(PathElement address) {
           throw ControllerLogger.ROOT_LOGGER.immutableResource();
       }

       @Override
       public boolean isRuntime() {
           return delegate.isRuntime();
       }

       @Override
       public boolean isProxy() {
           return delegate.isProxy();
       }

       @Override
       public Set<String> getOrderedChildTypes() {
           return delegate.getOrderedChildTypes();
       }

       public Resource clone() {
           return new ProtectedModelResource<Resource>(delegate.clone());
       }
   }


    private static class ProtectedModelResourceEntry extends ProtectedModelResource<ResourceEntry> implements ResourceEntry {

        ProtectedModelResourceEntry(ResourceEntry delegate){
            super(delegate);
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public PathElement getPathElement() {
            return delegate.getPathElement();
        }

        @Override
        public int hashCode() {
            return this.getPathElement().hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ResourceEntry)) return false;
            return this.getPathElement().equals(((ResourceEntry) object).getPathElement());
        }
    }
}
