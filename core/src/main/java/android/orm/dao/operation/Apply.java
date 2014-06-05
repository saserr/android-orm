/*
 * Copyright 2013 the original author or authors
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

package android.orm.dao.operation;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.orm.dao.Transaction;
import android.orm.util.Function;
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Apply extends Function.Base<Pair<String, Collection<Producer<ContentProviderOperation>>>, Maybe<Transaction.Result>> {

    private static final String TAG = Apply.class.getSimpleName();

    @NonNull
    private final ContentResolver mResolver;

    public Apply(@NonNull final ContentResolver resolver) {
        super();

        mResolver = resolver;
    }

    @NonNull
    @Override
    public final Maybe<Transaction.Result> invoke(@NonNull final Pair<String, Collection<Producer<ContentProviderOperation>>> pair) {
        final String authority = pair.first;
        final Collection<Producer<ContentProviderOperation>> batch = pair.second;
        final Maybe<Transaction.Result> result;

        try {
            if (batch.isEmpty()) {
                result = nothing();
            } else {
                final ArrayList<ContentProviderOperation> operations = new ArrayList<>(batch.size());
                for (final Producer<ContentProviderOperation> producer : batch) {
                    operations.add(producer.produce());
                }
                result = something(
                        new Transaction.Result(authority, mResolver.applyBatch(authority, operations))
                );
            }
        } catch (final RemoteException | OperationApplicationException cause) {
            @NonNls final String message = "There was a problem applying batches";
            Log.e(TAG, message, cause);
            throw Legacy.wrap(message, cause);
        }

        return result;
    }
}
