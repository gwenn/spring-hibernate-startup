package org.hibernate.persister;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.Mapping;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.LazyCollectionPersister;
import org.hibernate.persister.entity.LazyEntityPersister;
import org.hibernate.persister.entity.Queryable;

public class LazyPersisterFactory {
	private static final Class[] ENTITY_PERSISTER_CLASS = new Class[]{EntityPersister.class, ClassMetadata.class, Queryable.class};
	private static final Class[] COLLECTION_PERSISTER_CLASS = new Class[]{CollectionPersister.class, CollectionMetadata.class};
    private static Map proxies = Collections.synchronizedMap(new WeakHashMap());

	private LazyPersisterFactory() {
	}

	public static EntityPersister createClassPersister(
			PersistentClass model,
			CacheConcurrencyStrategy cache,
			SessionFactoryImpl factory,
			Mapping cfg) throws HibernateException {
		final LazyEntityPersister handler = new LazyEntityPersister(model, cache, factory, cfg);
		final Object proxy = Proxy.newProxyInstance(model.getClass().getClassLoader(),
				ENTITY_PERSISTER_CLASS, handler);
		handler.setClassMetadata((ClassMetadata) proxy);
        proxies.put(model.getEntityName(), Boolean.TRUE);
        return (EntityPersister) proxy;
	}
    public static boolean isLazyProxy(EntityPersister persister) {
        return null != persister && Proxy.isProxyClass(persister.getClass());
    }
    public static EntityPersister get(EntityPersister persister) {
        persister = ((LazyEntityPersister) Proxy.getInvocationHandler(persister)).getTarget();
        return persister;
    }

    public static CollectionPersister createCollectionPersister(
			Configuration cfg,
			Collection model,
			CacheConcurrencyStrategy cache,
			SessionFactoryImpl factory) throws HibernateException {
		final LazyCollectionPersister handler = new LazyCollectionPersister(cfg, model, cache, factory);
		final Object proxy = Proxy.newProxyInstance(model.getClass().getClassLoader(),
				COLLECTION_PERSISTER_CLASS, handler);
		handler.setCollectionMetadata((CollectionMetadata) proxy);
		return (CollectionPersister) proxy;
	}
    public static boolean isLazyProxy(CollectionPersister persister) {
        return null != persister && Proxy.isProxyClass(persister.getClass());
    }
    public static CollectionPersister get(CollectionPersister persister) {
        persister = ((LazyCollectionPersister) Proxy.getInvocationHandler(persister)).getTarget();
        return persister;
    }
}
