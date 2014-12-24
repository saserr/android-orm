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

package android.orm.dao;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.orm.Access;
import android.orm.DAO;
import android.orm.Database;
import android.orm.sql.Expression;
import android.orm.sql.Helper;
import android.orm.sql.Statement;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.UUID;

import static android.orm.util.Maybes.nothing;
import static android.util.Log.INFO;

public abstract class Direct implements DAO.Direct {

    private static final String TAG = Direct.class.getSimpleName();

    protected Direct() {
        super();
    }

    @NonNull
    @Override
    public final <K> Access.Direct.Single<K> access(@NonNull final Executor.Direct.Single.Factory<? super DAO.Direct, K> factory) {
        return new android.orm.dao.direct.Access.Single<>(factory.create(this));
    }

    @NonNull
    @Override
    public final <K> Access.Direct.Many<K> access(@NonNull final Executor.Direct.Many.Factory<? super DAO.Direct, K> factory) {
        return new android.orm.dao.direct.Access.Many<>(factory.create(this));
    }

    @NonNull
    public static DAO.Direct create(@NonNull final Context context,
                                    @NonNull final Database database) {
        return new OutsideTransaction(context, database);
    }

    public static class Interrupted extends SQLException {

        private static final long serialVersionUID = -6830868519893828093L;

        private Interrupted(@NonNls @NonNull final String error) {
            super(error);
        }
    }

    public static class InsideTransaction extends Direct {

        private static final Interrupted Interrupted = new Interrupted("Interrupted");

        @NonNull
        private final SQLiteDatabase mDatabase;

        public InsideTransaction(@NonNull final SQLiteDatabase database) {
            super();

            mDatabase = database;
        }

        @Override
        public final void execute(@NonNull final Statement statement) {
            interruptIfNecessary();
            statement.execute(mDatabase);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
            interruptIfNecessary();
            return expression.execute(mDatabase);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            interruptIfNecessary();
            Maybe<V> result = nothing();
            @NonNls final String savepoint = Helper.escape(UUID.randomUUID().toString());
            mDatabase.execSQL("savepoint " + savepoint + ';'); //NON-NLS

            try {
                result = transaction.run(this);
            } catch (final Transaction.Rollback ignored) {
                mDatabase.execSQL("rollback transaction to savepoint " + savepoint + ';'); //NON-NLS
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Subtransaction has been rolled back"); //NON-NLS
                }
            }

            return result;
        }

        private static void interruptIfNecessary() {
            if (Thread.interrupted()) {
                throw Interrupted;
            }
        }
    }

    public static class OutsideTransaction extends Direct {

        @NonNull
        private final Database.Helper mHelper;

        public OutsideTransaction(@NonNull final Context context,
                                  @NonNull final Database database) {
            super();

            mHelper = database.getHelper(context);
        }

        @Override
        public final void execute(@NonNull final Statement statement) {
            mHelper.execute(statement);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Expression<V> expression) {
            return mHelper.execute(expression);
        }

        @NonNull
        @Override
        public final <V> Maybe<V> execute(@NonNull final Transaction.Direct<V> transaction) {
            Maybe<V> result = nothing();

            final SQLiteDatabase database = mHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                result = transaction.run(new InsideTransaction(database));
                database.setTransactionSuccessful();
            } catch (final Transaction.Rollback ignored) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Transaction has been rolled back"); //NON-NLS
                }
            } finally {
                database.endTransaction();
            }

            return result;
        }
    }
}
