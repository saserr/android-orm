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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.model.Plan;
import android.orm.sql.Helper;
import android.orm.sql.Table;
import android.orm.sql.Value;
import android.orm.sql.Writables;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.util.Log.INFO;

public class Update implements Function<SQLiteDatabase, Maybe<Integer>> {

    private static final String TAG = Update.class.getSimpleName();

    @NonNull
    private final Table<?> mTable;
    @NonNull
    private final Select.Where mWhere;
    @NonNull
    private final Plan.Write mPlan;

    public Update(@NonNull final Table<?> table,
                  @NonNull final Select.Where where,
                  @NonNull final Plan.Write plan) {
        super();

        mTable = table;
        mWhere = where;
        mPlan = plan;
    }

    @NonNull
    @Override
    public final Maybe<Integer> invoke(@NonNull final SQLiteDatabase database) {
        final ContentValues values = new ContentValues();
        mPlan.write(Value.Write.Operation.Update, Writables.writable(values));
        final int updated;

        if (values.size() > 0) {
            updated = database.update(Helper.escape(mTable.getName()), values, mWhere.toSQL(), null);
        } else {
            updated = 0;
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was updated"); //NON-NLS
            }
        }

        return (updated > 0) ? Maybes.something(updated) : Maybes.<Integer>nothing();
    }

    public static class Notify implements Function<Integer, Integer> {

        @NonNull
        private final Notifier mNotifier;
        @NonNull
        private final Uri mUri;

        public Notify(@NonNull final Notifier notifier, @NonNull final Uri uri) {
            super();

            mNotifier = notifier;
            mUri = uri;
        }

        @NonNull
        @Override
        public final Integer invoke(@NonNull final Integer value) {
            mNotifier.notifyChange(mUri);
            return value;
        }
    }
}
