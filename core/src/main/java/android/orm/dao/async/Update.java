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

package android.orm.dao.async;

import android.orm.dao.Executor;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

public class Update implements ExecutionContext.Task<Object> {

    public static final ObjectPool<Update> Pool = new ObjectPool<Update>() {
        @NonNull
        @Override
        protected Update produce(@NonNull final Receipt<Update> receipt) {
            return new Update(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Update> mReceipt;

    private Executor.Direct<?, Object> mDirect;
    private Condition mCondition;
    private Writer mWriter;

    private Update(@NonNull final ObjectPool.Receipt<Update> receipt) {
        super();

        mReceipt = receipt;
    }

    @SuppressWarnings("unchecked")
    public final void init(@NonNull final Executor.Direct<?, ?> direct,
                           @NonNull final Condition condition,
                           @NonNull final Writer writer) {
        mDirect = (Executor.Direct<?, Object>) direct;
        mCondition = condition;
        mWriter = writer;
    }

    @NonNull
    @Override
    public final Maybe<Object> run() {
        final Maybe<Object> result;

        try {
            result = mDirect.update(mCondition, mWriter);
        } finally {
            mDirect = null;
            mCondition = null;
            mWriter = null;
            mReceipt.yield();
        }

        return result;
    }
}
