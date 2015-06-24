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
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Readables.combine;
import static android.orm.sql.Readables.readable;
import static android.orm.sql.Select.select;
import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Values.RowId;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Legacy.getKeys;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Insert implements Expression<Object> {

    public static final ObjectPool<Insert> Pool = new ObjectPool<Insert>() {
        @NonNull
        @Override
        protected Insert produce(@NonNull final Receipt<Insert> receipt) {
            return new Insert(receipt);
        }
    };

    private static final String TAG = Insert.class.getSimpleName();
    private static final Predicate.ComplexPart.WithNull<Long> WHERE_ROW_ID = Predicate.on(RowId);

    @NonNull
    private final ObjectPool.Receipt<Insert> mReceipt;

    @NonNls
    private String mTable;
    private Writer mWriter;
    private ContentValues mAdditional;
    private Value.Read<Object> mKey;

    private Insert(@NonNull final ObjectPool.Receipt<Insert> receipt) {
        super();

        mReceipt = receipt;
    }

    @SuppressWarnings("unchecked")
    public final void init(@NonNls @NonNull final String table,
                           @NonNull final Writer writer,
                           @NonNull final ContentValues additional,
                           @NonNull final Value.Read<?> key) {
        mTable = table;
        mWriter = writer;
        mAdditional = additional;
        mKey = Values.safeCast(key);
    }

    @NonNull
    @Override
    public final Maybe<Object> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Object> result;

        try {
            final ContentValues values = new ContentValues(mAdditional);
            final Writable output = writable(values);
            mWriter.write(Insert, output);
            if (values.size() <= 0) {
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "An empty row will be inserted"); //NON-NLS
                }
            }

            final long id = database.insertOrThrow(mTable, null, values);

            if (id > 0L) {
                final Maybe<Long> someId = something(id);
                RowId.write(Insert, someId, output);

                final Select.Projection remaining = mKey.getProjection().without(getKeys(values));
                if (remaining.isEmpty()) {
                    result = mKey.read(readable(values));
                } else {
                    final Predicate predicate = WHERE_ROW_ID.isEqualTo(id);
                    final Select select = select(mTable).with(predicate).with(Limit.Single).build();
                    final Readable input = select.execute(remaining, database);
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
                    throw new SQLException("Couldn't read item's key after insert");
                }
            } else {
                result = nothing();
            }
        } finally {
            mTable = null;
            mWriter = null;
            mAdditional = null;
            mKey = null;
            mReceipt.yield();
        }

        return result;
    }
}
