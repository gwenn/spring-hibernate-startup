package org.hibernate.persister.entity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.PersisterFactory;
import org.hibernate.persister.collection.CollectionPersister;

public class LazyCollectionPersister implements InvocationHandler {
	private static final Log log = LogFactory.getLog(LazyCollectionPersister.class);
	private static final String POST_INSTANTIATE = "postInstantiate";
	private static final String GET_COLLECTION_METADATA = "getCollectionMetadata";
	private static final String GET_INDEX_TYPE = "getIndexType";
	private static final String GET_ROLE = "getRole";
	private static final String GET_ELEMENT_TYPE = "getElementType";
	private static final String HAS_CACHE = "hasCache";
	private static final String GET_CACHE_ACCESS_STRATEGY = "getCacheAccessStrategy";

	private Collection model;
	private CacheConcurrencyStrategy cache;
	private SessionFactoryImpl factory;
	private Configuration cfg;

	private boolean postInstantiated = false;
	private CollectionMetadata collectionMetadata;

	public LazyCollectionPersister(Configuration cfg, Collection model, CacheConcurrencyStrategy cache, SessionFactoryImpl factory) {
		this.model = model;
		this.cache = cache;
		this.factory = factory;
		this.cfg = cfg;
	}

	public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
		final String methodName = method.getName();
		if (!postInstantiated) { // See SessionFactoryImpl#SessionFactoryImpl
			if (GET_COLLECTION_METADATA.equals(methodName)) {
				return collectionMetadata;
			} else if (GET_INDEX_TYPE.equals(methodName)) {
				if (model.isIndexed()) {
					return ((IndexedCollection) model).getIndex().getType();
				} else {
					return null;
				}
			} else if (GET_ROLE.equals(methodName)) {
				return model.getRole();
			} else if (GET_ELEMENT_TYPE.equals(methodName)) {
				return model.getElement().getType();
			} else if (POST_INSTANTIATE.equals(methodName)) {
				postInstantiated = true;
				return null;
			}
		}
		if (!postInstantiated) {
			throw new IllegalStateException("Unexpected method call before postInstantiate: " + methodName);
		}
		check();
		if (HAS_CACHE.equals(methodName)) { // See SessionFactoryImpl#close
			return cache != null;
		} else if (GET_CACHE_ACCESS_STRATEGY.equals(methodName)) {
			return cache;
		}
		throw new UnsupportedOperationException("Lazy mode...");
	}

	public CollectionPersister getTarget() {
		check();
		if (log.isInfoEnabled()) {
			log.info("Creating CollectionPersister for : " + model.getRole());
		}
		final CollectionPersister target = PersisterFactory.createCollectionPersister(cfg, model, cache, factory);
		clean();
		return target;
	}

	public void setCollectionMetadata(CollectionMetadata collectionMetadata) {
		this.collectionMetadata = collectionMetadata;
	}

	private void check() {
		if (null == model) {
			throw new IllegalStateException("LazyCollectionPersister instances should be discarded after first call of #getTarget");
		}
	}

	private void clean() {
		model = null;
		cache = null;
		factory = null;
		cfg = null;

		collectionMetadata = null;
	}
}
