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

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Expression;
import android.orm.sql.Readable;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Readables.combine;
import static android.orm.sql.Readables.readable;
import static android.orm.sql.Select.select;
import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Legacy.getKeys;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public final class Update {

    private static final String TAG = Update.class.getSimpleName();

    public static class Single implements Expression<Object> {

        public static final ObjectPool<Single> Pool = new ObjectPool<Single>() {
            @NonNull
            @Override
            protected Single produce(@NonNull final Receipt<Single> receipt) {
                return new Single(receipt);
            }
        };

        @NonNull
        private final ObjectPool.Receipt<Single> mReceipt;

        @NonNls
        private String mTable;
        private Predicate mPredicate;
        private Writer mWriter;
        private ContentValues mAdditional;
        private Value.Read<Object> mKey;
        private Select mSelect;

        private Single(@NonNull final ObjectPool.Receipt<Single> receipt) {
            super();

            mReceipt = receipt;
        }

        public final void init(@NonNls @NonNull final String table,
                               @NonNull final Predicate predicate,
                               @NonNull final Writer writer,
                               @NonNull final ContentValues additional,
                               @NonNull final Value.Read<?> key) {
            mTable = table;
            mPredicate = predicate.and(writer.onUpdate());
            mWriter = writer;
            mAdditional = additional;
            mKey = Values.safeCast(key);
            mSelect = select(table).with(predicate).with(Limit.Single).build();
        }

        @NonNull
        @Override
        public final Maybe<Object> execute(@NonNull final SQLiteDatabase database) {
            final Maybe<Object> result;

            try {
                final ContentValues values = new ContentValues();
                mWriter.write(Update, writable(values));
                final int updated = update(database, mTable, mPredicate, values);

                if (updated > 1) {
                    throw new SQLException("More than one row was updated");
                }

                if (updated > 0) {
                    values.putAll(mAdditional);
                    final Select.Projection remaining = mKey.getProjection().without(getKeys(values));
                    if (remaining.isEmpty()) {
                        result = mKey.read(readable(values));
                    } else {
                        final Readable input = mSelect.execute(remaining, database);
                        if ((input == null) || !input.start()) {
                            result = nothing();
                        } else {
                            try {
                                result = mKey.read(combine(readable(values), input));
                            } finally {
                                input.close();
                            }
                        }
                    }

                    if (result.isNothing()) {
                        throw new SQLException("Couldn't read row id after update");
                    }
                } else {
                    result = nothing();
                }
            } finally {
                mTable = null;
                mPredicate = null;
                mWriter = null;
                mAdditional = null;
                mKey = null;
                mSelect = null;
                mReceipt.yield();
            }

            return result;
        }
    }

    public static class Many implements Expression<Integer> {

        public static final ObjectPool<Many> Pool = new ObjectPool<Many>() {
            @NonNull
            @Override
            protected Many produce(@NonNull final Receipt<Many> receipt) {
                return new Many(receipt);
            }
        };

        @NonNull
        private final ObjectPool.Receipt<Many> mReceipt;

        @NonNls
        private String mTable;
        private Predicate mPredicate;
        private Writer mWriter;

        private Many(@NonNull final ObjectPool.Receipt<Many> receipt) {
            super();

            mReceipt = receipt;
        }

        public final void init(@NonNls @NonNull final String table,
                               @NonNull final Predicate predicate,
                               @NonNull final Writer writer) {
            mTable = table;
            mPredicate = predicate.and(writer.onUpdate());
            mWriter = writer;
        }

        @NonNull
        @Override
        public final Maybe<Integer> execute(@NonNull final SQLiteDatabase database) {
            final int updated;

            try {
                final ContentValues values = new ContentValues();
                mWriter.write(Update, writable(values));
                updated = update(database, mTable, mPredicate, values);
            } finally {
                mTable = null;
                mPredicate = null;
                mWriter = null;
                mReceipt.yield();
            }

            return (updated > 0) ? something(updated) : Maybes.<Integer>nothing();
        }
    }

    private static int update(@NonNull final SQLiteDatabase database,
                              @NonNls @NonNull final String table,
                              @NonNull final Predicate predicate,
                              @NonNull final ContentValues values) {
        final int updated;

        if (values.size() > 0) {
            updated = database.update(table, values, predicate.toSQL(), null);
        } else {
            updated = 0;
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was updated"); //NON-NLS
            }
        }

        return updated;
    }

    private Update() {
        super();
    }
}
