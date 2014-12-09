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
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.util.Maybe;
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

public class Insert<K> implements Expression<K> {

    private static final String TAG = Insert.class.getSimpleName();
    private static final Condition.ComplexPart.WithNull<Long> WHERE_ROW_ID = Condition.on(RowId);

    @NonNls
    @NonNull
    private final String mTable;
    @NonNull
    private final Writer mWriter;
    @NonNull
    private final ContentValues mAdditional;
    @NonNull
    private final Value.Read<K> mKey;

    public Insert(@NonNls @NonNull final String table,
                  @NonNull final Writer writer,
                  @NonNull final ContentValues additional,
                  @NonNull final Value.Read<K> key) {
        super();

        mTable = table;
        mWriter = writer;
        mAdditional = additional;
        mKey = key;
    }

    @NonNull
    @Override
    public final Maybe<K> execute(@NonNull final SQLiteDatabase database) {
        final ContentValues values = new ContentValues(mAdditional);
        final Writable output = writable(values);
        mWriter.write(Insert, output);
        if (values.size() <= 0) {
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "An empty row will be written"); //NON-NLS
            }
        }

        final long id = database.insertOrThrow(mTable, null, values);
        final Maybe<K> result;

        if (id > 0L) {
            final Maybe<Long> someId = something(id);
            RowId.write(Insert, someId, output);

            final Select.Projection remaining = mKey.getProjection().without(getKeys(values));
            if (remaining.isEmpty()) {
                result = mKey.read(readable(values));
            } else {
                final Condition condition = WHERE_ROW_ID.isEqualTo(id);
                final Select select = select(mTable).with(condition).with(Limit.Single).build();
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
                throw new SQLException("Couldn't create item uri after insert");
            }
        } else {
            result = nothing();
        }

        return result;
    }
}
