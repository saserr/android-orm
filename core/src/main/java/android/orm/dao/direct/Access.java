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

import android.orm.Model;
import android.orm.dao.Executor;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Plans;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;
import static android.orm.util.Maybes.something;

public final class Access {

    public static class Single<K> extends android.orm.dao.Access.Single<Maybe<Boolean>, Maybe<K>, Maybe<K>, Maybe<Integer>> implements android.orm.Access.Direct.Single<K> {

        @NonNull
        private final Executor.Direct.Single<K> mExecutor;

        public Single(@NonNull final Executor.Direct.Single<K> executor) {
            super(executor);

            mExecutor = executor;
        }

        @NonNull
        @Override
        public final <M extends Model> Maybe<M> query(@NonNull final M model) {
            return query(Model.toInstance(model)).map(Functions.constant(model));
        }

        @NonNull
        @Override
        public final <M extends Instance.Readable> Maybe<M> query(@NonNull final M model) {
            beforeRead(model);
            final Plan.Read<M> plan = Plans.single(model.getName(), Reading.Item.Update.from(model));
            final Maybe<M> result;

            if (plan.isEmpty()) {
                Observer.afterRead(model);
                result = something(model);
            } else {
                final Function<Producer<Maybe<M>>, Maybe<M>> afterRead = afterRead();
                result = mExecutor.query(plan, Where.None, null, null, null).flatMap(afterRead);
            }

            return result;
        }

        @NonNull
        @Override
        public final <M> Query.Builder<M> query(@NonNull final Value.Read<M> value) {
            return query(single(value));
        }

        @NonNull
        @Override
        public final <M> Query.Builder<M> query(@NonNull final Mapper.Read<M> mapper) {
            return query(single(mapper));
        }

        @NonNull
        @Override
        public final <M> Query.Builder<M> query(@NonNull final Reading.Single<M> reading) {
            return new Query.Builder<>(mExecutor, reading);
        }

        @Override
        protected final Maybe<K> afterCreate(@Nullable final Object model,
                                             @NonNull final Maybe<K> result) {
            if (result.getOrElse(null) != null) {
                Observer.afterCreate(model);
            }

            return result;
        }

        @Override
        protected final Maybe<K> afterUpdate(@Nullable final Object model,
                                             @NonNull final Maybe<K> result) {
            if (result.getOrElse(null) != null) {
                Observer.afterUpdate(model);
            }

            return result;
        }
    }

    public static class Many<K> extends android.orm.dao.Access.Many<Maybe<Boolean>, Maybe<K>, Maybe<Integer>, Maybe<Integer>> implements android.orm.Access.Direct.Many<K> {

        @NonNull
        private final Executor.Direct.Many<K> mExecutor;

        public Many(@NonNull final Executor.Direct.Many<K> executor) {
            super(executor);

            mExecutor = executor;
        }

        @NonNull
        @Override
        public final <M> Query.Builder<M> query(@NonNull final AggregateFunction<M> function) {
            return new Query.Builder<>(mExecutor, single(function));
        }

        @NonNull
        @Override
        public final <M> Query.Builder<List<M>> query(@NonNull final Value.Read<M> value) {
            return query(list(value));
        }

        @NonNull
        @Override
        public final <M> Query.Builder<List<M>> query(@NonNull final Mapper.Read<M> mapper) {
            return query(list(mapper));
        }

        @NonNull
        @Override
        public final <M> Query.Builder<M> query(@NonNull final Reading.Many<M> reading) {
            return new Query.Builder<>(mExecutor, reading);
        }

        @Override
        protected final Maybe<K> afterCreate(@Nullable final Object model,
                                             @NonNull final Maybe<K> result) {
            if (result.getOrElse(null) != null) {
                Observer.afterCreate(model);
            }

            return result;
        }

        @Override
        protected final Maybe<Integer> afterUpdate(@Nullable final Object model,
                                                   @NonNull final Maybe<Integer> result) {
            if (result.isSomething()) {
                final Integer updated = result.get();
                if ((updated != null) && (updated > 0)) {
                    Observer.afterUpdate(model);
                }
            }

            return result;
        }
    }

    private Access() {
        super();
    }
}
