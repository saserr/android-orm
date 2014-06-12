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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;

import static android.orm.sql.Readables.readable;

public class Read<V> extends Function.Base<SQLiteDatabase, Maybe<Producer<Maybe<V>>>> {

    private static final Object AfterRead = new Function.Base<Producer<Maybe<Object>>, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
            final Maybe<Object> result = producer.produce();
            if (result.isSomething()) {
                Observer.afterRead(result.get());
            }
            return result;
        }
    };

    @NonNull
    private final Plan.Read<V> mPlan;
    @NonNull
    private final Select mSelect;

    public Read(@NonNull final Plan.Read<V> plan, @NonNull final Select select) {
        super();

        mPlan = plan;
        mSelect = select;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<V>>> invoke(@NonNull final SQLiteDatabase database) {
        final Maybe<Producer<Maybe<V>>> result;

        final Cursor cursor = mSelect.execute(mPlan.getProjection(), database);
        if (cursor == null) {
            result = Maybes.nothing();
        } else {
            try {
                result = Maybes.something(mPlan.read(readable(cursor)));
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Function<Producer<Maybe<V>>, Maybe<V>> afterRead() {
        return (Function<Producer<Maybe<V>>, Maybe<V>>) AfterRead;
    }
}
