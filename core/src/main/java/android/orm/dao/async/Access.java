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
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Plans;
import android.orm.model.Reading;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.util.Consumer;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.dao.Result.something;
import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;

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
                result = mExecutor.query(plan, Condition.None, null, Limit.Single, null).flatMap(afterRead);
            }

            return result;
        }

        @NonNull
        @Override
        public final Query.Builder.Single query() {
            return new Query.Builder.Single(mExecutor);
        }

        @Override
        protected final Result<K> afterCreate(@Nullable final Object model,
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
        protected final Result<K> afterUpdate(@Nullable final Object model,
                                              @NonNull final Result<K> result) {
            return (model instanceof Observer.Write) ?
                    result.onComplete(new Result.Callback<K>() {
                        @Override
                        public void onResult(@NonNull final Maybe<K> key) {
                            if (key.getOrElse(null) != null) {
                                Observer.afterUpdate(model);
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
        public final Query.Builder.Many query() {
            return new Query.Builder.Many(mExecutor);
        }

        @Override
        protected final Result<K> afterCreate(@Nullable final Object model,
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
        protected final Result<Integer> afterUpdate(@Nullable final Object model,
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
