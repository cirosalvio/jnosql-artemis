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
package org.jnosql.artemis.column.query;


import org.jnosql.artemis.Converters;
import org.jnosql.artemis.Repository;
import org.jnosql.artemis.column.ColumnTemplate;
import org.jnosql.artemis.reflection.ClassRepresentation;
import org.jnosql.diana.api.column.ColumnDeleteQuery;
import org.jnosql.diana.api.column.ColumnQuery;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.jnosql.artemis.column.query.ColumnRepositoryType.getDeleteQuery;
import static org.jnosql.artemis.column.query.ColumnRepositoryType.getQuery;
import static org.jnosql.artemis.column.query.ReturnTypeConverterUtil.returnObject;
import static org.jnosql.diana.api.column.query.ColumnQueryBuilder.select;

/**
 * Template method to {@link Repository} proxy on column
 *
 * @param <T>  the entity type
 * @param <ID> the ID entity
 */
public abstract class AbstractColumnRepositoryProxy<T, ID> implements InvocationHandler {

    protected abstract Repository getRepository();

    protected abstract ClassRepresentation getClassRepresentation();

    protected abstract ColumnQueryParser getQueryParser();

    protected abstract ColumnQueryDeleteParser getDeleteParser();

    protected abstract ColumnTemplate getTemplate();

    protected abstract Converters getConverters();

    @Override
    public Object invoke(Object instance, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        ColumnRepositoryType type = ColumnRepositoryType.of(method, args);
        Class<?> typeClass = getClassRepresentation().getClassInstance();

        switch (type) {
            case DEFAULT:
                return method.invoke(getRepository(), args);
            case FIND_BY:
                ColumnQuery query = getQueryParser().parse(methodName, args, getClassRepresentation(),
                        getConverters());
                return returnObject(query, getTemplate(), typeClass, method);
            case FIND_ALL:
                return returnObject(select().from(getClassRepresentation().getName()).build(),
                        getTemplate(), typeClass, method);
            case DELETE_BY:
                ColumnDeleteQuery deleteQuery = getDeleteParser().parse(methodName, args, getClassRepresentation(),
                        getConverters());
                getTemplate().delete(deleteQuery);
                return Void.class;
            case QUERY:
                ColumnQuery columnQuery = getQuery(args).get();
                return returnObject(columnQuery, getTemplate(), typeClass, method);
            case QUERY_DELETE:
                ColumnDeleteQuery columnDeleteQuery = getDeleteQuery(args).get();
                getTemplate().delete(columnDeleteQuery);
                return Void.class;
            case OBJECT_METHOD:
                return method.invoke(this, args);
            default:
                return Void.class;

        }
    }

}
