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

package android.orm.dao.direct;

import android.database.sqlite.SQLiteDatabase;
import android.orm.Access;
import android.orm.dao.Executor;
import android.orm.model.Mapper;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Expression;
import android.orm.sql.Readable;
import android.orm.sql.Reader;
import android.orm.sql.Readers;
import android.orm.sql.Select;
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

import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Query implements Expression<Producer<Maybe<Object>>> {

    public static final ObjectPool<Query> Pool = new ObjectPool<Query>() {
        @NonNull
        @Override
        protected Query produce(@NonNull final Receipt<Query> receipt) {
            return new Query(receipt);
        }
    };

    private static final Object AfterRead = new Function<Producer<Maybe<Object>>, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
            final Maybe<Object> result = producer.produce();
            if (result.isSomething()) {
                Observer.afterRead(result.get());
            }
            return result;
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Query> mReceipt;

    private Reader.Collection<Object> mReader;
    private Select mSelect;

    private Query(@NonNull final ObjectPool.Receipt<Query> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final Reader.Collection<?> reader,
                           @NonNull final Select select) {
        mReader = Readers.safeCast(reader);
        mSelect = select;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<Object>>> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Producer<Maybe<Object>>> result;

        try {
            final Readable input = mSelect.execute(mReader.getProjection(), database);
            if (input == null) {
                result = nothing();
            } else {
                try {
                    result = something(mReader.read(input));
                } finally {
                    input.close();
                }
            }
        } finally {
            mReader = null;
            mSelect = null;
            mReceipt.yield();
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Function<Producer<Maybe<V>>, Maybe<V>> afterRead() {
        return (Function<Producer<Maybe<V>>, Maybe<V>>) AfterRead;
    }

    public static final class Builder {

        public static class Single implements Access.Direct.Query.Builder.Single {

            @NonNull
            private final Executor.Direct<?, ?> mExecutor;

            @NonNull
            private Predicate mPredicate = Predicate.None;

            public Single(@NonNull final Executor.Direct<?, ?> executor) {
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
            public final <V> Maybe<V> select(@NonNull final Value.Read<V> value) {
                return select(single(value));
            }

            @NonNull
            @Override
            public final <M> Maybe<M> select(@NonNull final Mapper.Read<M> mapper) {
                return select(single(mapper));
            }

            @NonNull
            @Override
            public final <M> Maybe<M> select(@NonNull final M model,
                                             @NonNull final Mapper.Read<M> mapper) {
                return select(model, single(mapper));
            }

            @NonNull
            @Override
            public final <M> Maybe<M> select(@NonNull final Reading.Single<M> reading) {
                return select(reading.prepareReader());
            }

            @NonNull
            @Override
            public final <M> Maybe<M> select(@NonNull final M model,
                                             @NonNull final Reading.Single<M> reading) {
                beforeRead(model);
                return select(reading.prepareReader(model));
            }

            @NonNull
            @Override
            public final <V> Maybe<V> select(@NonNull final Reader.Collection<V> reader) {
                final Maybe<Producer<Maybe<V>>> result = mExecutor.query(reader, mPredicate, null, Limit.Single, null);
                return result.flatMap(Query.<V>afterRead());
            }
        }

        public static class Many implements Access.Direct.Query.Builder.Many {

            @NonNull
            private final Executor.Direct<?, ?> mExecutor;

            @NonNull
            private Predicate mPredicate = Predicate.None;
            @Nullable
            private Order mOrder;
            @Nullable
            private Limit mLimit;
            @Nullable
            private Offset mOffset;

            public Many(@NonNull final Executor.Direct<?, ?> executor) {
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
            public final <V> Maybe<V> select(@NonNull final AggregateFunction<V> function) {
                return select(Readers.single(function.getName(), Plan.Read.from(function)));
            }

            @NonNull
            @Override
            public final <V> Maybe<List<V>> select(@NonNull final Value.Read<V> value) {
                return select(list(value));
            }

            @NonNull
            @Override
            public final <M> Maybe<List<M>> select(@NonNull final Mapper.Read<M> mapper) {
                return select(list(mapper));
            }

            @NonNull
            @Override
            public final <M> Maybe<M> select(@NonNull final Reading.Many<M> reading) {
                return select(reading.prepareReader());
            }

            @NonNull
            @Override
            public final <V> Maybe<V> select(@NonNull final Reader.Collection<V> reader) {
                final Maybe<Producer<Maybe<V>>> result = mExecutor.query(reader, mPredicate, mOrder, mLimit, mOffset);
                return result.flatMap(Query.<V>afterRead());
            }
        }

        private Builder() {
            super();
        }
    }
}
