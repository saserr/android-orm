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
import android.orm.dao.async.ExecutionContext;
import android.orm.sql.Reader;
import android.orm.sql.Select;
import android.orm.sql.fragment.Condition;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.orm.sql.Readables.limit;
import static android.orm.sql.Readables.readable;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Query implements ExecutionContext.Task<Producer<Maybe<Object>>> {

    public static final ObjectPool<Query> Pool = new ObjectPool<Query>() {
        @NonNull
        @Override
        protected Query produce(@NonNull final Receipt<Query> receipt) {
            return new Query(receipt);
        }
    };

    private static final String TAG = Query.class.getSimpleName();

    @NonNull
    private final ObjectPool.Receipt<Query> mReceipt;

    private ContentResolver mResolver;
    private Uri mUri;
    private Reader<Object> mReader;
    private Condition mCondition;
    private Order mOrder;
    private Limit mLimit;
    private Offset mOffset;

    private Query(@NonNull final ObjectPool.Receipt<Query> receipt) {
        super();

        mReceipt = receipt;
    }

    @SuppressWarnings("unchecked")
    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Reader<?> reader,
                           @NonNull final Condition condition) {
        init(resolver, uri, reader, condition, null, null, null);
    }

    @SuppressWarnings("unchecked")
    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Reader<?> reader,
                           @NonNull final Condition condition,
                           @Nullable final Order order,
                           @Nullable final Limit limit,
                           @Nullable final Offset offset) {
        mResolver = resolver;
        mUri = uri;
        mReader = (Reader<Object>) reader;
        mCondition = condition;
        mOrder = order;
        mLimit = limit;
        mOffset = offset;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<Object>>> run() {
        final Maybe<Producer<Maybe<Object>>> result;

        try {
            final Select.Projection projection = mReader.getProjection();

            if (projection.isEmpty()) {
                result = nothing();
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Nothing was queried"); //NON-NLS
                }
            } else {
                final String where = mCondition.toSQL();
                final String order = (mOrder == null) ? null : mOrder.toSQL();
                final Cursor cursor = mResolver.query(mUri, projection.asArray(), where, null, order);
                if (cursor == null) {
                    result = nothing();
                } else {
                    try {
                        result = something(mReader.read(limit(readable(cursor), mLimit, mOffset)));
                    } finally {
                        cursor.close();
                    }
                }
            }
        } finally {
            mResolver = null;
            mUri = null;
            mReader = null;
            mCondition = null;
            mOrder = null;
            mLimit = null;
            mOffset = null;
            mReceipt.yield();
        }

        return result;
    }
}
