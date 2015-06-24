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
import android.content.ContentValues;
import android.net.Uri;
import android.orm.dao.async.ExecutionContext;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;
import android.util.Log;

import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;
import static android.util.Log.INFO;

public class Update implements ExecutionContext.Task<Integer> {

    public static final ObjectPool<Update> Pool = new ObjectPool<Update>() {
        @NonNull
        @Override
        protected Update produce(@NonNull final Receipt<Update> receipt) {
            return new Update(receipt);
        }
    };

    private static final String TAG = Update.class.getSimpleName();

    @NonNull
    private final ObjectPool.Receipt<Update> mReceipt;

    private ContentResolver mResolver;
    private Uri mUri;
    private Predicate mPredicate;
    private Writer mWriter;

    private Update(@NonNull final ObjectPool.Receipt<Update> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Predicate predicate,
                           @NonNull final Writer writer) {
        mResolver = resolver;
        mUri = uri;
        mPredicate = predicate;
        mWriter = writer;
    }

    @NonNull
    @Override
    public final Maybe<Integer> run() {
        final int updated;

        try {
            final ContentValues values = new ContentValues();
            mWriter.write(Update, writable(values));

            if (values.size() > 0) {
                final Predicate predicate = mPredicate.and(mWriter.onUpdate());
                updated = mResolver.update(mUri, values, predicate.toSQL(), null);
            } else {
                updated = 0;
                if (Log.isLoggable(TAG, INFO)) {
                    Log.i(TAG, "Nothing was updated"); //NON-NLS
                }
            }
        } finally {
            mResolver = null;
            mUri = null;
            mPredicate = null;
            mWriter = null;
            mReceipt.yield();
        }

        return (updated > 0) ? something(updated) : Maybes.<Integer>nothing();
    }
}
