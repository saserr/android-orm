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

package android.orm.remote.dao;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.net.Uri;
import android.orm.Access;
import android.orm.Remote;
import android.orm.dao.Executor;
import android.orm.reactive.Route;
import android.orm.remote.dao.direct.Apply;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import org.jetbrains.annotations.NonNls;

import java.util.Collection;

public class Direct implements Remote.Direct {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Apply mApply;

    public Direct(@NonNull final ContentResolver resolver) {
        super();

        mResolver = resolver;
        mApply = new Apply(resolver);
    }

    @NonNull
    @Override
    public final Access.Direct.Single<Uri> at(@NonNull final Route.Single route,
                                              @NonNull final Object... arguments) {
        return access(Remote.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Direct.Many<Uri> at(@NonNull final Route.Many route,
                                            @NonNull final Object... arguments) {
        return access(Remote.at(route, arguments));
    }

    @NonNull
    @Override
    public final Access.Direct.Many<Uri> at(@NonNull final Uri uri) {
        return access(Remote.at(uri));
    }

    @NonNull
    @Override
    public final <K> Access.Direct.Single<K> access(@NonNull final Executor.Direct.Single.Factory<ContentResolver, K> factory) {
        return new android.orm.dao.direct.Access.Single<>(factory.create(mResolver));
    }

    @NonNull
    @Override
    public final <K> Access.Direct.Many<K> access(@NonNull final Executor.Direct.Many.Factory<ContentResolver, K> factory) {
        return new android.orm.dao.direct.Access.Many<>(factory.create(mResolver));
    }

    @NonNull
    @Override
    public final Transaction<Maybe<Transaction.CommitResult>> transaction() {
        return new Transaction<Maybe<Transaction.CommitResult>>() {
            @NonNull
            @Override
            protected Maybe<Transaction.CommitResult> commit(@NonNls @Nullable final String authority,
                                                             @NonNull final Collection<Producer<ContentProviderOperation>> batch) {
                return ((authority == null) || batch.isEmpty()) ?
                        Maybes.<Transaction.CommitResult>nothing() :
                        mApply.invoke(Pair.create(authority, batch));
            }
        };
    }

    @NonNull
    public static Remote.Direct create(@NonNull final ContentResolver resolver) {
        return new Direct(resolver);
    }
}
