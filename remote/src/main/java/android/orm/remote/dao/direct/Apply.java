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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.orm.dao.async.ExecutionContext;
import android.orm.remote.dao.Transaction;
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.ObjectPool;
import android.orm.util.Producer;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.util.Maybes.nothing;
import static java.lang.System.arraycopy;

public class Apply implements ExecutionContext.Task<Transaction.CommitResult> {

    public static final ObjectPool<Apply> Pool = new ObjectPool<Apply>() {
        @NonNull
        @Override
        protected Apply produce(@NonNull final Receipt<Apply> receipt) {
            return new Apply(receipt);
        }
    };

    private static final String TAG = Apply.class.getSimpleName();

    @NonNull
    private final ObjectPool.Receipt<Apply> mReceipt;

    private ContentResolver mResolver;
    @NonNls
    private String mAuthority;
    private Collection<Producer<ContentProviderOperation>> mBatch;

    private Apply(@NonNull final ObjectPool.Receipt<Apply> receipt) {
        super();

        mReceipt = receipt;
    }

    public final void init(@NonNull final ContentResolver resolver,
                           @NonNls @NonNull final String authority,
                           @NonNull final Collection<Producer<ContentProviderOperation>> batch) {
        mResolver = resolver;
        mAuthority = authority;
        mBatch = batch;
    }

    @NonNull
    @Override
    public final Maybe<Transaction.CommitResult> run() {
        final Maybe<Transaction.CommitResult> result;

        try {
            if (mBatch.isEmpty()) {
                result = nothing();
            } else {
                final ArrayList<ContentProviderOperation> operations = new ArrayList<>(mBatch.size());
                for (final Producer<ContentProviderOperation> producer : mBatch) {
                    operations.add(producer.produce());
                }
                result = Maybes.<Transaction.CommitResult>something(
                        new Result(mAuthority, mResolver.applyBatch(mAuthority, operations))
                );
            }
        } catch (final RemoteException | OperationApplicationException cause) {
            @NonNls final String message = "There was a problem applying batches";
            Log.e(TAG, message, cause);
            throw Legacy.wrap(message, cause);
        } finally {
            mResolver = null;
            mAuthority = null;
            mBatch = null;
            mReceipt.yield();
        }

        return result;
    }

    private static class Result implements Transaction.CommitResult {

        @NonNull
        private final String mAuthority;
        @NonNull
        private final ContentProviderResult[] mResults;

        private Result(@NonNull final String authority,
                       @NonNull final ContentProviderResult... results) {
            super();

            mAuthority = authority;
            mResults = new ContentProviderResult[results.length];
            arraycopy(results, 0, mResults, 0, mResults.length);
        }

        @NonNls
        @NonNull
        @Override
        public final String getAuthority() {
            return mAuthority;
        }

        @NonNull
        @Override
        public final ContentProviderResult[] getResults() {
            final ContentProviderResult[] results = new ContentProviderResult[mResults.length];
            arraycopy(mResults, 0, results, 0, results.length);
            return results;
        }
    }
}
