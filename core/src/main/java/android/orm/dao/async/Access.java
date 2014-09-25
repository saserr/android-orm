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

import android.orm.Model;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Plans;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.fragment.Where;
import android.orm.util.Consumer;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import static android.orm.dao.Result.something;
import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;
import static android.orm.model.Readings.list;
import static android.orm.model.Readings.single;

public final class Access {

    public static class Single<K> extends android.orm.dao.Access.Single<Result<Boolean>, Result<K>, Result<K>, Result<Integer>> implements android.orm.Access.Async.Single<K> {

        @NonNull
        private final Executor.Async.Single<K> mExecutor;

        public Single(@NonNull final Executor.Async.Single<K> executor) {
            super(executor);

            mExecutor = executor;
        }

        @NonNull
        @Override
        public final <M extends Model> Result<M> query(@NonNull final M model) {
            return query(Model.toInstance(model)).map(Functions.constant(model));
        }

        @NonNull
        @Override
        public final <M extends Instance.Readable> Result<M> query(@NonNull final M model) {
            beforeRead(model);
            final Plan.Read<M> plan = Plans.single(model.getName(), Reading.Item.Update.from(model));
            final Result<M> result;

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
        protected final <M> Result<K> afterCreate(@Nullable final M model,
                                                  @NonNull final Result<K> result) {
            return (model instanceof Observer.Write) ?
                    result.onComplete(new Result.Callback<K>() {
                        @Override
                        public void onResult(@NonNull final Maybe<K> key) {
                            if (key.getOrElse(null) != null) {
                                Observer.afterCreate(model);
                            }
                        }
                    }) :
                    result;
        }

        @Override
        protected final <M> Result<K> afterUpdate(@Nullable final M model,
                                                  @NonNull final Result<K> result) {
            return (model instanceof Observer.Write) ?
                    result.onComplete(new Result.Callback<K>() {
                        @Override
                        public void onResult(@NonNull final Maybe<K> key) {
                            if (key.getOrElse(null) != null) {
                                Observer.afterCreate(model);
                            }
                        }
                    }) :
                    result;
        }
    }

    public static class Many<K> extends android.orm.dao.Access.Many<Result<Boolean>, Result<K>, Result<Integer>, Result<Integer>> implements android.orm.Access.Async.Many<K> {

        @NonNull
        private final Executor.Async.Many<K> mExecutor;

        public Many(@NonNull final Executor.Async.Many<K> executor) {
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
        protected final <M> Result<K> afterCreate(@Nullable final M model,
                                                  @NonNull final Result<K> result) {
            return (model instanceof Observer.Write) ?
                    result.onComplete(new Result.Callback<K>() {
                        @Override
                        public void onResult(@NonNull final Maybe<K> key) {
                            if (key.getOrElse(null) != null) {
                                Observer.afterCreate(model);
                            }
                        }
                    }) :
                    result;
        }

        @Override
        protected final <M> Result<Integer> afterUpdate(@Nullable final M model,
                                                        @NonNull final Result<Integer> result) {
            return (model instanceof Observer.Write) ?
                    result.onSomething(new Consumer<Integer>() {
                        @Override
                        public void consume(@Nullable final Integer updated) {
                            if ((updated != null) && (updated > 0)) {
                                Observer.afterUpdate(model);
                            }
                        }
                    }) :
                    result;
        }
    }

    private Access() {
        super();
    }
}
