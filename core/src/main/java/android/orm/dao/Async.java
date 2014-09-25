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
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutorService;

import static android.orm.dao.async.Executors.create;
import static android.orm.util.Maybes.nothing;

public class Async implements DAO.Async {

    @NonNull
    private final DAO.Direct mDirectDAO;
    @NonNull
    private final android.orm.dao.async.Executor mExecutor;

    public Async(@NonNull final DAO.Direct dao, @NonNull final ExecutorService executor) {
        super();

        mDirectDAO = dao;
        mExecutor = new android.orm.dao.async.Executor(executor);
    }

    @Override
    public final void setErrorHandler(@Nullable final ErrorHandler handler) {
        mExecutor.setErrorHandler(handler);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Single<K> executor = create(mExecutor, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Single<>(executor);
    }

    @NonNull
    @Override
    public final <K> Access.Async.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super DAO.Direct, K> factory) {
        final Executor.Async.Many<K> executor = create(mExecutor, factory.create(mDirectDAO));
        return new android.orm.dao.async.Access.Many<>(executor);
    }

    @NonNull
    @Override
    public final Result<Void> execute(@NonNull final Statement statement) {
        return mExecutor.execute(new Producer<Maybe<Void>>() {
            @NonNull
            @Override
            public Maybe<Void> produce() {
                mDirectDAO.execute(statement);
                return nothing();
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Expression<V> expression) {
        return mExecutor.execute(new Producer<Maybe<V>>() {
            @NonNull
            @Override
            public Maybe<V> produce() {
                return mDirectDAO.execute(expression);
            }
        });
    }

    @NonNull
    @Override
    public final <V> Result<V> execute(@NonNull final Transaction.Direct<V> transaction) {
        return mExecutor.execute(new Producer<Maybe<V>>() {
            @NonNull
            @Override
            public Maybe<V> produce() {
                return mDirectDAO.execute(transaction);
            }
        });
    }
}
