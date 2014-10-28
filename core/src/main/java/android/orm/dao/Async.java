/*
 * Copyright 2014 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.orm.dao;

import android.orm.Access;
import android.orm.DAO;
import android.orm.dao.async.ExecutionContext;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;
import static android.orm.util.Maybes.nothing;

public class Async implements DAO.Async {

    @NonNull
    private final DAO.Direct mDirectDAO;
    @NonNull
    private final ExecutionContext mExecutionContext;

    public Async(@NonNull final DAO.Direct dao, @NonNull final ExecutorService executor) {
        super();

        mDirectDAO = dao;
        mExecutionContext = new ExecutionContext(executor);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutionContext.setErrorHandler(handler);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Single<K> executor = create(mExecutionContext, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Single<>(executor);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Many<K> executor = create(mExecutionContext, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Many<>(executor);
    }

    @NonNull
    @Override
    public final Result<Void> execute(@NonNull final Statement statement) {
        return mExecutionContext.execute(new ExecutionContext.Task<Void>() {
            @NonNull
            @Override
            public Maybe<Void> run() {
                mDirectDAO.execute(statement);
                return nothing();
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return mExecutionContext.execute(new ExecutionContext.Task<V>() {
            @NonNull
            @Override
            public Maybe<V> run() {
                return mDirectDAO.execute(expression);
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        return mExecutionContext.execute(new ExecutionContext.Task<V>() {
            @NonNull
            @Override
            public Maybe<V> run() {
                return mDirectDAO.execute(transaction);
            }
        });
    }

}
