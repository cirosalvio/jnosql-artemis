/*
 *  Copyright (c) 2017 Otávio Santana and others
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.jnosql.artemis.document.spi;


import org.jnosql.artemis.Repository;
import org.jnosql.artemis.RepositoryAsync;
import org.jnosql.artemis.Database;
import org.jnosql.artemis.Databases;
import org.jnosql.artemis.document.query.DocumentRepositoryAsyncBean;
import org.jnosql.artemis.document.query.RepositoryDocumentBean;
import org.jnosql.diana.api.document.DocumentCollectionManager;
import org.jnosql.diana.api.document.DocumentCollectionManagerAsync;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessProducer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.jnosql.artemis.DatabaseType.DOCUMENT;

/**
 * Extension to start up the DocumentTemplate, DocumentTemplateAsync, Repository and RepositoryAsync
 * from the {@link Database} qualifier
 */
public class DocumentCollectionProducerExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(DocumentCollectionProducerExtension.class.getName());

    private final List<Database> databases = new ArrayList<>();

    private final List<Database> databasesAsync = new ArrayList<>();

    private final Collection<Class<?>> crudTypes = new HashSet<>();

    private final Collection<Class<?>> crudAsyncTypes = new HashSet<>();


    <T extends Repository> void onProcessAnnotatedType(@Observes final ProcessAnnotatedType<T> repo) {
        Class<T> javaClass = repo.getAnnotatedType().getJavaClass();
        if (Repository.class.equals(javaClass)) {
            return;
        }


        if (Stream.of(javaClass.getInterfaces()).anyMatch(Repository.class::equals)
                && Modifier.isInterface(javaClass.getModifiers())) {
            LOGGER.info("Adding a new Repository as discovered on document: " + javaClass);
            crudTypes.add(javaClass);
        }
    }

    <T extends RepositoryAsync> void onProcessAnnotatedTypeAsync(@Observes final ProcessAnnotatedType<T> repo) {
        Class<T> javaClass = repo.getAnnotatedType().getJavaClass();

        if (RepositoryAsync.class.equals(javaClass)) {
            return;
        }

        if (Stream.of(javaClass.getInterfaces()).anyMatch(RepositoryAsync.class::equals)
                && Modifier.isInterface(javaClass.getModifiers())) {
            LOGGER.info("Adding a new RepositoryAsync as discovered on document: " + javaClass);
            crudAsyncTypes.add(javaClass);
        }
    }

    <T, X extends DocumentCollectionManager> void processProducer(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, DOCUMENT, databases);
    }

    <T, X extends DocumentCollectionManagerAsync> void processProducerAsync(@Observes final ProcessProducer<T, X> pp) {
        Databases.addDatabase(pp, DOCUMENT, databasesAsync);
    }

    void onAfterBeanDiscovery(@Observes final AfterBeanDiscovery afterBeanDiscovery, final BeanManager beanManager) {
        LOGGER.info(String.format("Starting to process on documents: %d databases crud %d and crudAsync %d",
                databases.size(), crudTypes.size(), crudAsyncTypes.size()));

        databases.forEach(type -> {
            final DocumentTemplateBean bean = new DocumentTemplateBean(beanManager, type.provider());
            afterBeanDiscovery.addBean(bean);
        });

        databasesAsync.forEach(type -> {
            final DocumentTemplateAsyncBean bean = new DocumentTemplateAsyncBean(beanManager, type.provider());
            afterBeanDiscovery.addBean(bean);
        });

        crudTypes.forEach(type -> {
            afterBeanDiscovery.addBean(new RepositoryDocumentBean(type, beanManager, ""));
            databases.forEach(database -> {
                final RepositoryDocumentBean bean = new RepositoryDocumentBean(type, beanManager, database.provider());
                afterBeanDiscovery.addBean(bean);
            });
        });

        crudAsyncTypes.forEach(type -> {
            afterBeanDiscovery.addBean(new DocumentRepositoryAsyncBean(type, beanManager, ""));
            databasesAsync.forEach(database -> {
                final DocumentRepositoryAsyncBean bean = new DocumentRepositoryAsyncBean(type, beanManager, database.provider());
                afterBeanDiscovery.addBean(bean);
            });
        });

    }

}
