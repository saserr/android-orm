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

package android.orm.dao.async;

import android.orm.Access;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Reader;
import android.orm.sql.Readers;
import android.orm.sql.Value;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;

public class Query implements ExecutionContext.Task<Producer<Maybe<Object>>> {

    public static final ObjectPool<Query> Pool = new ObjectPool<Query>() {
        @NonNull
        @Override
        protected Query produce(@NonNull final Receipt<Query> receipt) {
            return new Query(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Query> mReceipt;

    private Executor.Direct<?, ?> mDirect;
    private Reader.Collection<Object> mReader;
    private Predicate mPredicate;
    private Order mOrder;
    private Limit mLimit;
    private Offset mOffset;

    private Query(@NonNull final ObjectPool.Receipt<Query> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final Executor.Direct<?, ?> direct,
                           @NonNull final Reader.Collection<?> reader,
                           @NonNull final Predicate predicate,
                           @Nullable final Order order,
                           @Nullable final Limit limit,
                           @Nullable final Offset offset) {
        mDirect = direct;
        mReader = Readers.safeCast(reader);
        mPredicate = predicate;
        mOrder = order;
        mLimit = limit;
        mOffset = offset;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<Object>>> run() {
        final Maybe<Producer<Maybe<Object>>> result;

        try {
            result = mDirect.query(mReader, mPredicate, mOrder, mLimit, mOffset);
        } finally {
            mDirect = null;
            mReader = null;
            mPredicate = null;
            mOrder = null;
            mLimit = null;
            mOffset = null;
            mReceipt.yield();
        }

        return result;
    }

    public static final class Builder {
        public static class Single implements Access.Async.Query.Builder.Single {

            @NonNull
            private final Executor.Async<?, ?> mExecutor;

            @NonNull
            private Predicate mPredicate = Predicate.None;

            public Single(@NonNull final Executor.Async<?, ?> executor) {
                super();

                mExecutor = executor;
            }

            @NonNull
            @Override
            public final Single with(@Nullable final Predicate predicate) {
                mPredicate = (predicate == null) ? Predicate.None : predicate;
                return this;
            }

            @NonNull
            @Override
            public final <V> Result<V> select(@NonNull final Value.Read<V> value) {
                return select(single(value));
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final Mapper.Read<M> mapper) {
                return select(single(mapper));
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final M model,
                                              @NonNull final Mapper.Read<M> mapper) {
                return select(model, single(mapper));
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final Reading.Single<M> reading) {
                return select(reading.prepareReader());
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final M model,
                                              @NonNull final Reading.Single<M> reading) {
                beforeRead(model);
                return select(reading.prepareReader(model));
            }

            @NonNull
            @Override
            public final <V> Result<V> select(@NonNull final Reader.Collection<V> reader) {
                final Function<Producer<Maybe<V>>, Maybe<V>> afterRead = afterRead();
                return mExecutor.query(reader, mPredicate, null, Limit.Single, null).flatMap(afterRead);
            }
        }

        public static class Many implements Access.Async.Query.Builder.Many {

            @NonNull
            private final Executor.Async<?, ?> mExecutor;

            @NonNull
            private Predicate mPredicate = Predicate.None;
            @Nullable
            private Order mOrder;
            @Nullable
            private Limit mLimit;
            @Nullable
            private Offset mOffset;

            public Many(@NonNull final Executor.Async<?, ?> executor) {
                super();

                mExecutor = executor;
            }

            @NonNull
            @Override
            public final Many with(@Nullable final Predicate predicate) {
                mPredicate = (predicate == null) ? Predicate.None : predicate;
                return this;
            }

            @NonNull
            @Override
            public final Many with(@Nullable final Order order) {
                mOrder = order;
                return this;
            }

            @NonNull
            @Override
            public final Many with(@Nullable final Limit limit) {
                mLimit = limit;
                return this;
            }

            @NonNull
            @Override
            public final Many with(@Nullable final Offset offset) {
                mOffset = offset;
                return this;
            }

            @NonNull
            @Override
            public final <V> Result<V> select(@NonNull final AggregateFunction<V> function) {
                return select(Readers.single(function.getName(), Plan.Read.from(function)));
            }

            @NonNull
            @Override
            public final <V> Result<List<V>> select(@NonNull final Value.Read<V> value) {
                return select(list(value));
            }

            @NonNull
            @Override
            public final <M> Result<List<M>> select(@NonNull final Mapper.Read<M> mapper) {
                return select(list(mapper));
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final Reading.Many<M> reading) {
                return select(reading.prepareReader());
            }

            @NonNull
            @Override
            public final <V> Result<V> select(@NonNull final Reader.Collection<V> reader) {
                final Function<Producer<Maybe<V>>, Maybe<V>> afterRead = afterRead();
                return mExecutor.query(reader, mPredicate, mOrder, mLimit, mOffset).flatMap(afterRead);
            }
        }


        private Builder() {
            super();
        }
    }
}
