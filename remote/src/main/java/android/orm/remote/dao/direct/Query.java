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

package android.orm.remote.dao.direct;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.orm.model.Plan;
import android.orm.sql.Select;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.orm.sql.Readables.readable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Query<M> implements Function<Query.Arguments<M>, Maybe<Producer<Maybe<M>>>> {

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
    public final Maybe<Producer<Maybe<M>>> invoke(@NonNull final Arguments<M> arguments) {
        final Plan.Read<M> plan = arguments.getPlan();
        final Select.Projection projection = plan.getProjection();
        final Maybe<Producer<Maybe<M>>> result;

        if (projection.isEmpty()) {
            result = nothing();
            if (Log.isLoggable(TAG, INFO)) {
                Log.i(TAG, "Nothing was queried"); //NON-NLS
            }
        } else {
            final String where = arguments.getWhere().toSQL();
            final Order order = arguments.getOrder();
            final Cursor cursor = mResolver.query(mUri, projection.asArray(), where, null, (order == null) ? null : order.toSQL());
            if (cursor == null) {
                result = nothing();
            } else {
                try {
                    final Limit limit = arguments.getLimit();
                    final Offset offset = arguments.getOffset();
                    result = something(plan.read(readable(cursor, limit, offset)));
                } finally {
                    cursor.close();
                }
            }
        }

        return result;
    }

    public static class Arguments<M> {

        @NonNull
        private final Plan.Read<M> mPlan;
        @NonNull
        private final Where mWhere;
        @Nullable
        private final Order mOrder;
        @Nullable
        private final Limit mLimit;
        @Nullable
        private final Offset mOffset;

        public Arguments(@NonNull final Plan.Read<M> plan) {
            this(plan, Where.None, null, null, null);
        }

        public Arguments(@NonNull final Plan.Read<M> plan,
                         @NonNull final Where where,
                         @Nullable final Order order,
                         @Nullable final Limit limit,
                         @Nullable final Offset offset) {
            super();

            mPlan = plan;
            mWhere = where;
            mOrder = order;
            mLimit = limit;
            mOffset = offset;
        }

        @NonNull
        public final <T> Arguments<T> copy(@NonNull final Plan.Read<T> plan) {
            return new Arguments<>(plan, mWhere, mOrder, mLimit, mOffset);
        }

        @NonNull
        private Plan.Read<M> getPlan() {
            return mPlan;
        }

        @NonNull
        private Where getWhere() {
            return mWhere;
        }

        @Nullable
        private Order getOrder() {
            return mOrder;
        }

        @Nullable
        private Limit getLimit() {
            return mLimit;
        }

        @Nullable
        private Offset getOffset() {
            return mOffset;
        }
    }
}
