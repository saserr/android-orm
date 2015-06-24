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
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

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

    private Executor.Direct<?, ?> mDirect;
    private Predicate mPredicate;

    private Delete(@NonNull final ObjectPool.Receipt<Delete> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final Executor.Direct<?, ?> direct,
                           @NonNull final Predicate predicate) {
        mDirect = direct;
        mPredicate = predicate;
    }

    @NonNull
    @Override
    public final Maybe<Integer> run() {
        final Maybe<Integer> result;

        try {
            result = mDirect.delete(mPredicate);
        } finally {
            mDirect = null;
            mPredicate = null;
            mReceipt.yield();
        }

        return result;
    }
}
