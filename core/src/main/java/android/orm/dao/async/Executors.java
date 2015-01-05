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
import android.orm.sql.Reader;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public final class Executors {

    @NonNull
    public static <K> Executor.Async.Single<K> create(@NonNull final ExecutionContext context,
                                                      @NonNull final Executor.Direct.Single<K> direct) {
        return new Single<>(context, direct);
    }

    @NonNull
    public static <K> Executor.Async.Many<K> create(@NonNull final ExecutionContext context,
                                                    @NonNull final Executor.Direct.Many<K> direct) {
        return new Many<>(context, direct);
    }

    private static class Single<K> extends Some<K, K> implements Executor.Async.Single<K> {
        private Single(@NonNull final ExecutionContext context,
                       @NonNull final Executor.Direct.Single<K> direct) {
            super(context, direct);
        }
    }

    private static class Many<K> extends Some<K, Integer> implements Executor.Async.Many<K> {
        private Many(@NonNull final ExecutionContext context,
                     @NonNull final Executor.Direct.Many<K> direct) {
            super(context, direct);
        }
    }

    private abstract static class Some<I, U> implements Executor.Async<I, U> {

        @NonNull
        private final ExecutionContext mExecutionContext;
        @NonNull
        private final Executor.Direct<I, U> mDirect;

        private Some(@NonNull final ExecutionContext context,
                     @NonNull final Executor.Direct<I, U> direct) {
            super();

            mExecutionContext = context;
            mDirect = direct;
        }

        @NonNull
        @Override
        public final Result<Boolean> exists(@NonNull final Condition condition) {
            final Exists exists = Exists.Pool.borrow();
            exists.init(mDirect, condition);
            return mExecutionContext.execute(exists);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <M> Result<Producer<Maybe<M>>> query(@NonNull final Reader.Collection<M> reader,
                                                          @NonNull final Condition condition,
                                                          @Nullable final Order order,
                                                          @Nullable final Limit limit,
                                                          @Nullable final Offset offset) {
            final Query query = Query.Pool.borrow();
            query.init(mDirect, reader, condition, order, limit, offset);
            return (Result<Producer<Maybe<M>>>) (Object) mExecutionContext.execute(query);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final Result<I> insert(@NonNull final Writer writer) {
            final Insert insert = Insert.Pool.borrow();
            insert.init(mDirect, writer);
            return (Result<I>) (Object) mExecutionContext.execute(insert);
        }

        @NonNull
        @Override
        public final Result<Integer> delete(@NonNull final Condition condition) {
            final Delete delete = Delete.Pool.borrow();
            delete.init(mDirect, condition);
            return mExecutionContext.execute(delete);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final Result<U> update(@NonNull final Condition condition,
                                      @NonNull final Writer writer) {
            final Update update = Update.Pool.borrow();
            update.init(mDirect, condition, writer);
            return (Result<U>) (Object) mExecutionContext.execute(update);
        }
    }

    private Executors() {
        super();
    }
}
