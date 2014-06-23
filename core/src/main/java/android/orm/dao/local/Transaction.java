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

package android.orm.dao.local;

import android.content.ContentResolver;
import android.database.sqlite.SQLiteDatabase;
import android.orm.DAO;
import android.orm.dao.direct.Notifier;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.orm.util.Maybes.nothing;
import static android.util.Log.INFO;

public abstract class Transaction<V> {

    private static final String TAG = Transaction.class.getSimpleName();
    private static final Rollback Rollback = new Rollback();

    @NonNull
    protected abstract Maybe<V> run(@NonNull final DAO.Direct dao) throws Rollback;

    @NonNull
    public final Maybe<V> execute(@NonNull final ContentResolver resolver,
                                  @NonNull final SQLiteDatabase database) {
        Maybe<V> result = nothing();

        try {
            final Notifier.Delayed notifier = new Notifier.Delayed(resolver);
            final DAO.Direct dao = DAO.direct(database, notifier);
            result = run(dao);
            notifier.sendAll();
        } catch (final Rollback ignored) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Transaction has been rolled back"); //NON-NLS
            }
        }

        return result;
    }

    protected static void rollback() throws Rollback {
        throw Rollback;
    }

    public static class Rollback extends Exception {

        private static final long serialVersionUID = -5591631583529668826L;

        private Rollback() {
            super("Rollback");
        }
    }
}
