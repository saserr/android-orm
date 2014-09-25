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

import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.model.Plan;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Executors {

    @NonNull
    public static <K> Executor.Async.Single<K> create(@NonNull final android.orm.dao.async.Executor executor,
                                                      @NonNull final Executor.Direct.Single<K> direct) {
        return new Single<>(executor, direct);
    }

    @NonNull
    public static <K> Executor.Async.Many<K> create(@NonNull final android.orm.dao.async.Executor executor,
                                                    @NonNull final Executor.Direct.Many<K> direct) {
        return new Many<>(executor, direct);
    }

    private static class Single<K> extends Some<K, K> implements Executor.Async.Single<K> {
        private Single(@NonNull final android.orm.dao.async.Executor executor,
                       @NonNull final Executor.Direct.Single<K> direct) {
            super(executor, direct);
        }
    }

    private static class Many<K> extends Some<K, Integer> implements Executor.Async.Many<K> {
        private Many(@NonNull final android.orm.dao.async.Executor executor,
                     @NonNull final Executor.Direct.Many<K> direct) {
            super(executor, direct);
        }
    }

    private abstract static class Some<I, U> implements Executor.Async<I, U> {

        @NonNull
        private final android.orm.dao.async.Executor mExecutor;
        @NonNull
        private final Executor.Direct<I, U> mDirect;

        private Some(@NonNull final android.orm.dao.async.Executor executor,
                     @NonNull final Executor.Direct<I, U> direct) {
            super();

            mExecutor = executor;
            mDirect = direct;
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Where where) {
            return mExecutor.execute(new Producer<Maybe<Boolean>>() {
                @NonNull
                @Override
                public Maybe<Boolean> produce() {
                    return mDirect.exists(where);
                }
            });
        }

        @NonNull
        @Override
        public final <M> Result<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                          @NonNull final Where where,
                                                          @Nullable final Order order,
                                                          @Nullable final Limit limit,
                                                          @Nullable final Offset offset) {
            return mExecutor.execute(new Producer<Maybe<Producer<Maybe<M>>>>() {
                @NonNull
                @Override
                public Maybe<Producer<Maybe<M>>> produce() {
                    return mDirect.query(plan, where, order, limit, offset);
                }
            });
        }

        @NonNull
        @Override
        public final Result<I> insert(@NonNull final Plan.Write plan) {
            return mExecutor.execute(new Producer<Maybe<I>>() {
                @NonNull
                @Override
                public Maybe<I> produce() {
                    return mDirect.insert(plan);
                }
            });
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Where where) {
            return mExecutor.execute(new Producer<Maybe<Integer>>() {
                @NonNull
                @Override
                public Maybe<Integer> produce() {
                    return mDirect.delete(where);
                }
            });
        }

        @NonNull
        @Override
        public final Result<U> update(@NonNull final Where where, @NonNull final Plan.Write plan) {
            return mExecutor.execute(new Producer<Maybe<U>>() {
                @NonNull
                @Override
                public Maybe<U> produce() {
                    return mDirect.update(where, plan);
                }
            });
        }
    }

    private Executors() {
        super();
    }
}
