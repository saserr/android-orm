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

package android.orm.reactive.dao;

import android.net.Uri;
import android.orm.dao.Executor;
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
    public static Executor.Direct.Single<Uri> create(@NonNull final Executor.Direct.Single<Uri> executor,
                                                     @NonNull final Notifier notifier,
                                                     @NonNull final Uri uri) {
        return new Single(executor, notifier, uri);
    }

    @NonNull
    public static Executor.Direct.Many<Uri> create(@NonNull final Executor.Direct.Many<Uri> executor,
                                                   @NonNull final Notifier notifier,
                                                   @NonNull final Uri uri) {
        return new Many(executor, notifier, uri);
    }

    private static class Single extends Some<Uri> implements Executor.Direct.Single<Uri> {

        @NonNull
        private final Executor.Direct.Single<Uri> mExecutor;

        private Single(@NonNull final Executor.Direct.Single<Uri> executor,
                       @NonNull final Notifier notifier,
                       @NonNull final Uri uri) {
            super(executor, notifier, uri);

            mExecutor = executor;
        }

        @NonNull
        @Override
        public final Maybe<Uri> update(@NonNull final Where where,
                                       @NonNull final Plan.Write plan) {
            final Maybe<Uri> result = mExecutor.update(where, plan);
            notifyChange(result);
            return result;
        }
    }

    private static class Many extends Some<Integer> implements Executor.Direct.Many<Uri> {

        @NonNull
        private final Executor.Direct.Many<Uri> mExecutor;

        private Many(@NonNull final Executor.Direct.Many<Uri> executor,
                     @NonNull final Notifier notifier,
                     @NonNull final Uri uri) {
            super(executor, notifier, uri);

            mExecutor = executor;
        }

        @NonNull
        @Override
        public final Maybe<Integer> update(@NonNull final Where where,
                                           @NonNull final Plan.Write plan) {
            final Maybe<Integer> result = mExecutor.update(where, plan);
            notifyIfChanged(result);
            return result;
        }
    }

    private abstract static class Some<U> implements Executor.Direct<Uri, U> {

        @NonNull
        private final Executor.Direct<Uri, U> mExecutor;
        @NonNull
        private final Notifier mNotifier;
        @NonNull
        private final Uri mUri;

        protected Some(@NonNull final Executor.Direct<Uri, U> executor,
                       @NonNull final Notifier notifier,
                       @NonNull final Uri uri) {
            super();

            mExecutor = executor;
            mNotifier = notifier;
            mUri = uri;
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Where where) {
            return mExecutor.exists(where);
        }

        @NonNull
        @Override
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Where where,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            return mExecutor.query(plan, where, order, limit, offset);
        }

        @NonNull
        @Override
        public final Maybe<Uri> insert(@NonNull final Plan.Write plan) {
            final Maybe<Uri> result = mExecutor.insert(plan);
            notifyChange(result);
            return result;
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Where where) {
            final Maybe<Integer> result = mExecutor.delete(where);
            notifyIfChanged(result);
            return result;
        }

        protected final void notifyIfChanged(@NonNull final Maybe<Integer> result) {
            if (result.isSomething()) {
                final Integer changed = result.get();
                if ((changed != null) && (changed > 0)) {
                    notifyChange();
                }
            }
        }

        protected final void notifyChange(@NonNull final Maybe<Uri> uri) {
            if (uri.isSomething()) {
                notifyChange(uri.get());
            }
        }

        protected final void notifyChange(@Nullable final Uri uri) {
            if (uri == null) {
                notifyChange();
            } else {
                mNotifier.notifyChange(uri);
            }
        }

        protected final void notifyChange() {
            mNotifier.notifyChange(mUri);
        }
    }

    private Executors() {
        super();
    }
}
