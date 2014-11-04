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

package android.orm.playground.reactive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.orm.Access;
import android.orm.DAO;
import android.orm.Database;
import android.orm.dao.Executor;
import android.orm.playground.Reactive;
import android.orm.remote.Route;
import android.orm.sql.Expression;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

public abstract class Direct implements Reactive.Direct {

    @NonNull
    private final DAO.Direct mDAO;
    @NonNull
    private final Notifier mNotifier;

    protected Direct(@NonNull final DAO.Direct dao, @NonNull final Notifier notifier) {
        super();

        mDAO = dao;
        mNotifier = notifier;
    }

    @NonNull
    public final Notifier getNotifier() {
        return mNotifier;
    }

    @NonNull
    @Override
    public final Access.Direct.Single<Uri> at(@NonNull final Route.Single route,
                                              @NonNull final Object... arguments) {
        return access(route.createUri(arguments), Reactive.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Direct.Many<Uri> at(@NonNull final Route.Many route,
                                            @NonNull final Object... arguments) {
        return access(route.createUri(arguments), Reactive.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Direct.Single<Uri> access(@NonNull final Uri uri,
                                                  @NonNull final Executor.Direct.Single.Factory<? super Reactive.Direct, Uri> factory) {
        final Executor.Direct.Single<Uri> executor = factory.create(this);
        return new android.orm.dao.direct.Access.Single<>(Executors.create(executor, mNotifier, uri));
    }

    @NonNull
    @Override
    public final Access.Direct.Many<Uri> access(@NonNull final Uri uri,
                                                @NonNull final Executor.Direct.Many.Factory<? super Reactive.Direct, Uri> factory) {
        final Executor.Direct.Many<Uri> executor = factory.create(this);
        return new android.orm.dao.direct.Access.Many<>(Executors.create(executor, mNotifier, uri));
    }

    @Override
    public final void execute(@NonNull final Statement statement) {
        mDAO.execute(statement);
    }

    @NonNull
    @Override
    public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
        return mDAO.execute(expression);
    }

    @NonNull
    public static Direct create(@NonNull final Context context, @NonNull final Database database) {
        final DAO.Direct dao = android.orm.dao.Direct.create(context, database);
        final ContentResolver resolver = context.getContentResolver();
        return new OutsideTransaction(dao, resolver);
    }

    public static class InsideTransaction extends Direct {

        @NonNull
        private final DAO.Direct mDAO;
        @NonNull
        private final Notifier mNotifier;

        public InsideTransaction(@NonNull final DAO.Direct dao,
                                 @NonNull final Notifier notifier) {
            super(dao, notifier);

            mDAO = dao;
            mNotifier = notifier;
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            return mDAO.execute(new android.orm.dao.Transaction.Direct<V>() {
                @NonNull
                @Override
                public Maybe<V> run(@NonNull final DAO.Direct dao) throws android.orm.dao.Transaction.Rollback {
                    return transaction.run(new InsideTransaction(dao, mNotifier));
                }
            });
        }
    }

    public static class OutsideTransaction extends Direct {

        @NonNull
        private final DAO.Direct mDAO;
        @NonNull
        private final ContentResolver mResolver;

        public OutsideTransaction(@NonNull final DAO.Direct dao,
                                  @NonNull final ContentResolver resolver) {
            super(dao, new Notifier.Immediate(resolver));

            mResolver = resolver;
            mDAO = dao;
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            return mDAO.execute(new android.orm.dao.Transaction.Direct<V>() {
                @NonNull
                @Override
                public Maybe<V> run(@NonNull final DAO.Direct dao) throws android.orm.dao.Transaction.Rollback {
                    final Notifier.Delayed notifier = new Notifier.Delayed(mResolver);
                    final Maybe<V> result = transaction.run(new InsideTransaction(dao, notifier));
                    notifier.sendAll();
                    return result;
                }
            });
        }
    }
}
