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
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public class Exists implements ExecutionContext.Task<Boolean> {

    public static final ObjectPool<Exists> Pool = new ObjectPool<Exists>() {
        @NonNull
        @Override
        protected Exists produce(@NonNull final Receipt<Exists> receipt) {
            return new Exists(receipt);
        }
    };

    private static final String[] PROJECTION = {"1"};

    @NonNull
    private final ObjectPool.Receipt<Exists> mReceipt;

    private ContentResolver mResolver;
    private Uri mUri;
    private Condition mCondition;

    private Exists(@NonNull final ObjectPool.Receipt<Exists> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Condition condition) {
        mResolver = resolver;
        mUri = uri;
        mCondition = condition;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> run() {
        final Maybe<Boolean> result;

        Cursor cursor = null;
        try {
            cursor = mResolver.query(mUri, PROJECTION, mCondition.toSQL(), null, null);
            result = something((cursor != null) && (cursor.getCount() > 0));
        } finally {
            mResolver = null;
            mUri = null;
            mCondition = null;
            mReceipt.yield();

            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }
}
