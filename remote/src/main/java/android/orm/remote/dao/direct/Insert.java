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
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;

public class Insert implements ExecutionContext.Task<Uri> {

    public static final ObjectPool<Insert> Pool = new ObjectPool<Insert>() {
        @NonNull
        @Override
        protected Insert produce(@NonNull final Receipt<Insert> receipt) {
            return new Insert(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Insert> mReceipt;

    private ContentResolver mResolver;
    private Uri mUri;
    private Writer mWriter;

    private Insert(@NonNull final ObjectPool.Receipt<Insert> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final ContentResolver resolver,
                           @NonNull final Uri uri,
                           @NonNull final Writer writer) {
        mResolver = resolver;
        mUri = uri;
        mWriter = writer;
    }

    @NonNull
    @Override
    public final Maybe<Uri> run() {
        final Uri result;

        try {
            final ContentValues values = new ContentValues();
            mWriter.write(Insert, writable(values));
            result = mResolver.insert(mUri, values);
        } finally {
            mResolver = null;
            mUri = null;
            mWriter = null;
            mReceipt.yield();
        }

        return (result == null) ? Maybes.<Uri>nothing() : something(result);
    }
}
