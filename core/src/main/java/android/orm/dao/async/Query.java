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
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.dao.Result.nothing;
import static android.orm.dao.Result.something;
import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;

public final class Query {

    public static final class Builder {
        public static class Single implements Access.Async.Query.Builder.Single {

            @NonNull
            private final Executor.Async<?, ?> mExecutor;

            @NonNull
            private Condition mCondition = Condition.None;

            public Single(@NonNull final Executor.Async<?, ?> executor) {
                super();

                mExecutor = executor;
            }

            @NonNull
            @Override
            public final Single with(@Nullable final Condition condition) {
                mCondition = (condition == null) ? Condition.None : condition;
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
                return select(null, reading.preparePlan());
            }

            @NonNull
            @Override
            public final <M> Result<M> select(@NonNull final M model,
                                              @NonNull final Reading.Single<M> reading) {
                beforeRead(model);
                return select(model, reading.preparePlan(model));
            }

            @NonNull
            private <M> Result<M> select(@Nullable final M model,
                                         @NonNull final Plan.Read<M> plan) {
                final Result<M> result;

                if (plan.isEmpty()) {
                    Observer.afterRead(model);
                    result = (model == null) ? Result.<M>nothing() : something(model);
                } else {
                    final Function<Producer<Maybe<M>>, Maybe<M>> afterRead = afterRead();
                    result = mExecutor.query(plan, mCondition, null, Limit.Single, null).flatMap(afterRead);
                }

                return result;
            }
        }

        public static class Many implements Access.Async.Query.Builder.Many {

            @NonNull
            private final Executor.Async<?, ?> mExecutor;

            @NonNull
            private Condition mCondition = Condition.None;
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
            public final Many with(@Nullable final Condition condition) {
                mCondition = (condition == null) ? Condition.None : condition;
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
                return select(single(function));
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
                return select((Reading<M>) reading);
            }

            @NonNull
            private <M> Result<M> select(@NonNull final Reading<M> reading) {
                final Plan.Read<M> plan = reading.preparePlan();
                final Result<M> result;

                if (plan.isEmpty()) {
                    result = nothing();
                } else {
                    final Function<Producer<Maybe<M>>, Maybe<M>> afterRead = afterRead();
                    result = mExecutor.query(plan, mCondition, mOrder, mLimit, mOffset).flatMap(afterRead);
                }

                return result;
            }
        }


        private Builder() {
            super();
        }
    }

    private Query() {
        super();
    }
}
