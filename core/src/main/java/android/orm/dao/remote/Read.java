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

package android.orm.dao.remote;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.orm.model.Plan;
import android.orm.sql.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.orm.sql.Readables.readable;
import static android.util.Log.INFO;

public class Read<V> implements Function<Read.Arguments<V>, Maybe<Producer<Maybe<V>>>> {

    private static final String TAG = Read.class.getSimpleName();

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Read(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<V>>> invoke(@NonNull final Arguments<V> arguments) {
        final Plan.Read<V> plan = arguments.getPlan();
        final Select.Projection projection = plan.getProjection();
        final Maybe<Producer<Maybe<V>>> result;

        if (projection.isEmpty()) {
            result = Maybes.nothing();
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
        } else {
            final String where = arguments.getWhere().toSQL();
            final Select.Order order = arguments.getOrder();
            final Cursor cursor = mResolver.query(mUri, projection.asArray(), where, null, (order == null) ? null : order.toSQL());
            if (cursor == null) {
                result = Maybes.nothing();
            } else {
                try {
                    result = Maybes.something(plan.read(readable(cursor)));
                } finally {
                    cursor.close();
                }
            }
        }

        return result;
    }

    public static class Arguments<V> {

        @NonNull
        private final Plan.Read<V> mPlan;
        @NonNull
        private final Select.Where mWhere;
        @Nullable
        private final Select.Order mOrder;

        public Arguments(@NonNull final Plan.Read<V> plan,
                         @NonNull final Select.Where where,
                         @Nullable final Select.Order order) {
            super();

            mPlan = plan;
            mWhere = where;
            mOrder = order;
        }

        @NonNull
        public final <T> Arguments<T> copy(@NonNull final Plan.Read<T> plan) {
            return new Arguments<>(plan, mWhere, mOrder);
        }

        @NonNull
        private Plan.Read<V> getPlan() {
            return mPlan;
        }

        @NonNull
        private Select.Where getWhere() {
            return mWhere;
        }

        @Nullable
        private Select.Order getOrder() {
            return mOrder;
        }
    }
}
