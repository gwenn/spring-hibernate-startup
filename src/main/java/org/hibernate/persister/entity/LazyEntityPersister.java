package org.hibernate.persister.entity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.engine.Mapping;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.PersisterFactory;

public class LazyEntityPersister implements InvocationHandler {
	private static final Log log = LogFactory.getLog(LazyEntityPersister.class);
	private static final String POST_INSTANTIATE = "postInstantiate";
	private static final String GET_CLASS_METADATA = "getClassMetadata";
	private static final String HAS_CACHE = "hasCache";
	private static final String GET_CACHE_ACCESS_STRATEGY = "getCacheAccessStrategy";
	private static final String GET_ENTITY_NAME = "getEntityName";
	private static final String IS_EXPLICIT_POLYMORPHISM = "isExplicitPolymorphism";
	private static final String GET_MAPPED_CLASS = "getMappedClass";
	private static final String IS_INHERITED = "isInherited";
	private static final String GET_MAPPED_SUPER_CLASS = "getMappedSuperclass";

	private PersistentClass model;
	private CacheConcurrencyStrategy cache;
	private SessionFactoryImpl factory;
	private Mapping cfg;

	private boolean postInstantiated = false;
	private ClassMetadata classMetadata;

	public LazyEntityPersister(PersistentClass model, CacheConcurrencyStrategy cache, SessionFactoryImpl factory, Mapping cfg) {
		this.model = model;
		this.cache = cache;
		this.factory = factory;
		this.cfg = cfg;
	}

	public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
		final String methodName = method.getName();
		if (!postInstantiated) { // See SessionFactoryImpl#SessionFactoryImpl
			if (GET_CLASS_METADATA.equals(methodName)) {
				return classMetadata;
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
		} else if (GET_ENTITY_NAME.equals(methodName)) { // See SessionFactoryImpl#getImplementors
			return model.getEntityName();
		} else if (IS_EXPLICIT_POLYMORPHISM.equals(methodName)) {
			return model.isExplicitPolymorphism();
		} else if (GET_MAPPED_CLASS.equals(methodName)) {
			return model.getMappedClass();
		} else if (IS_INHERITED.equals(methodName)) {
			return model.isInherited();
		} else if (GET_MAPPED_SUPER_CLASS.equals(methodName)) {
			return model.isInherited() ? model.getSuperclass().getEntityName() : null;
		}

		throw new UnsupportedOperationException("Lazy mode...");
	}

	public EntityPersister getTarget() {
		check();
		if (log.isInfoEnabled()) {
			log.info("Creating ClassPersister for: " + model.getEntityName());
		}
		final EntityPersister target = PersisterFactory.createClassPersister(model, cache, factory, cfg);
		clean();
		return target;
	}

	public void setClassMetadata(ClassMetadata classMetadata) {
		this.classMetadata = classMetadata;
	}

	private void check() {
		if (null == model) {
			throw new IllegalStateException("LazyEntityPersister instances should be discarded after first call of #getTarget");
		}
	}

	private void clean() {
		model = null;
		cache = null;
		factory = null;
		cfg = null;

		classMetadata = null;
	}
}
