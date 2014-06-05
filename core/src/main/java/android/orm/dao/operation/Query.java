/*
 * Copyright 2013 the original author or authors
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

package android.orm.dao.operation;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.orm.model.Plan;
import android.orm.sql.Readable;
import android.orm.sql.Readables;
import android.orm.sql.Reader;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Query<V> extends Function.Base<Query.Arguments<V>, Maybe<Pair<Reader<V>, Readable>>> {

    private static final String TAG = Query.class.getSimpleName();

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Query(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Pair<Reader<V>, Readable>> invoke(@NonNull final Arguments<V> arguments) {
        final Select.Projection projection = arguments.plan.getProjection();
        @org.jetbrains.annotations.Nullable final Cursor cursor;

        if (projection.isEmpty()) {
            cursor = null;
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
        } else {
            final String order = (arguments.order == null) ? null : arguments.order.toSQL();
            cursor = mResolver.query(mUri, projection.asArray(), arguments.where.toSQL(), null, order);
        }

        return (cursor == null) ?
                Maybes.<Pair<Reader<V>, Readable>>nothing() :
                something(Pair.<Reader<V>, Readable>create(arguments.plan, Readables.readable(cursor)));
    }

    public static class Arguments<M> {

        @NonNull
        public final Plan.Read<M> plan;
        @NonNull
        public final Select.Where where;
        @Nullable
        public final Select.Order order;

        public Arguments(@NonNull final Plan.Read<M> plan,
                         @NonNull final Select.Where where,
                         @Nullable final Select.Order order) {
            super();

            this.plan = plan;
            this.where = where;
            this.order = order;
        }
    }
}
