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
import android.net.Uri;
import android.orm.dao.async.ExecutionContext;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public class Delete implements ExecutionContext.Task<Integer> {

    public static final ObjectPool<Delete> Pool = new ObjectPool<Delete>() {
        @NonNull
        @Override
        protected Delete produce(@NonNull final Receipt<Delete> receipt) {
            return new Delete(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Delete> mReceipt;

    private ContentResolver mResolver;
    private Uri mUri;
    private Predicate mPredicate;

    private Delete(@NonNull final ObjectPool.Receipt<Delete> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Predicate predicate) {
        mResolver = resolver;
        mUri = uri;
        mPredicate = predicate;
    }

    @NonNull
    @Override
    public final Maybe<Integer> run() {
        final int deleted;

        try {
            deleted = mResolver.delete(mUri, mPredicate.toSQL(), null);
        } finally {
            mResolver = null;
            mUri = null;
            mPredicate = null;
            mReceipt.yield();
        }

        return (deleted > 0) ? something(deleted) : Maybes.<Integer>nothing();
    }
}
