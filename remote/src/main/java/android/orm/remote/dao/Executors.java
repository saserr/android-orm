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

package android.orm.remote.dao;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.dao.Executor;
import android.orm.model.Plan;
import android.orm.remote.Route;
import android.orm.remote.dao.direct.Delete;
import android.orm.remote.dao.direct.Exists;
import android.orm.remote.dao.direct.Insert;
import android.orm.remote.dao.direct.Query;
import android.orm.remote.dao.direct.Update;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import static android.orm.util.Maybes.something;

public final class Executors {

    @NonNull
    public static Executor.Direct.Single<Uri> single(@NonNull final ContentResolver resolver,
                                                     @NonNull final Route.Single route,
                                                     @NonNull final Object... arguments) {
        return new Single(resolver, route.createUri(arguments));
    }

    @NonNull
    public static Executor.Direct.Many<Uri> many(@NonNull final ContentResolver resolver,
                                                 @NonNull final Route.Many route,
                                                 @NonNull final Object... arguments) {
        return many(resolver, route.createUri(arguments));
    }

    @NonNull
    public static Executor.Direct.Many<Uri> many(@NonNull final ContentResolver resolver,
                                                 @NonNull final Uri uri) {
        return new Many(resolver, uri);
    }

    private static class Single extends Some<Uri> implements Executor.Direct.Single<Uri> {

        private static final String TAG = Route.Single.class.getSimpleName();

        @NonNull
        private final Update mUpdate;
        @NonNull
        private final Function<Integer, Maybe<Uri>> mToUri;

        protected Single(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
            super(resolver, uri);

            mUpdate = new Update(resolver, uri);
            mToUri = new Function<Integer, Maybe<Uri>>() {
                @NonNull
                @Override
                public Maybe<Uri> invoke(@NonNull final Integer updated) {
                    if (updated > 1) {
                        Log.w(TAG, "More than one row was updated! Single access is not actually access for single rows"); //NON-NLS
                    }

                    return (updated > 0) ? something(uri) : Maybes.<Uri>nothing();
                }
            };
        }

        @NonNull
        @Override
        public final Maybe<Uri> update(@NonNull final Condition condition,
                                       @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<Uri>nothing() :
                    mUpdate.invoke(Pair.<Condition, Writer>create(condition, plan)).flatMap(mToUri);
        }
    }

    private static class Many extends Some<Integer> implements Executor.Direct.Many<Uri> {

        @NonNull
        private final Update mUpdate;

        protected Many(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
            super(resolver, uri);

            mUpdate = new Update(resolver, uri);
        }

        @NonNull
        @Override
        public final Maybe<Integer> update(@NonNull final Condition condition,
                                           @NonNull final Plan.Write plan) {
            return plan.isEmpty() ?
                    Maybes.<Integer>nothing() :
                    mUpdate.invoke(Pair.<Condition, Writer>create(condition, plan));
        }
    }

    private abstract static class Some<U> implements Executor.Direct<Uri, U> {

        @NonNull
        private final Exists mExists;
        @NonNull
        private final Object mQuery;
        @NonNull
        private final Insert mInsert;
        @NonNull
        private final Delete mDelete;

        protected Some(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
            super();

            mExists = new Exists(resolver, uri);
            mQuery = new Query<>(resolver, uri);
            mInsert = new Insert(resolver, uri);
            mDelete = new Delete(resolver, uri);
        }

        @NonNull
        @Override
        public final Maybe<Boolean> exists(@NonNull final Condition condition) {
            return mExists.invoke(condition);
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                                         @NonNull final Condition condition,
                                                         @Nullable final Order order,
                                                         @Nullable final Limit limit,
                                                         @Nullable final Offset offset) {
            final Query.Arguments<M> arguments = new Query.Arguments<>(plan, condition, order, limit, offset);
            return ((Query<M>) mQuery).invoke(arguments);
        }

        @NonNull
        @Override
        public final Maybe<Uri> insert(@NonNull final Plan.Write plan) {
            return plan.isEmpty() ? Maybes.<Uri>nothing() : mInsert.invoke(plan);
        }

        @NonNull
        @Override
        public final Maybe<Integer> delete(@NonNull final Condition condition) {
            return mDelete.invoke(condition);
        }
    }

    private Executors() {
        super();
    }
}
