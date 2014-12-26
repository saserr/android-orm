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
import android.orm.sql.fragment.Condition;
import android.orm.util.Maybe;
import android.orm.util.ObjectPool;
import android.support.annotation.NonNull;

public class Exists implements ExecutionContext.Task<Boolean> {

    public static final ObjectPool<Exists> Pool = new ObjectPool<Exists>() {
        @NonNull
        @Override
        protected Exists produce(@NonNull final Receipt<Exists> receipt) {
            return new Exists(receipt);
        }
    };

    @NonNull
    private final ObjectPool.Receipt<Exists> mReceipt;

    private Executor.Direct<?, ?> mDirect;
    private Condition mCondition;

    private Exists(@NonNull final ObjectPool.Receipt<Exists> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final Executor.Direct<?, ?> direct,
                           @NonNull final Condition condition) {
        mDirect = direct;
        mCondition = condition;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> run() {
        final Maybe<Boolean> result;

        try {
            result = mDirect.exists(mCondition);
        } finally {
            mDirect = null;
            mCondition = null;
            mReceipt.yield();
        }

        return result;
    }
}
