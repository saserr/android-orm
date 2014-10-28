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

package android.orm.dao;

import android.orm.model.Plan;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Cancelable;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.orm.util.Promise;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public interface Executor<E, I, U, D> {

    @NonNull
    E exists(@NonNull final Where where);

    @NonNull
    I insert(@NonNull final Plan.Write plan);

    @NonNull
    U update(@NonNull final Where where, @NonNull final Plan.Write plan);

    @NonNull
    D delete(@NonNull final Where where);

    interface Direct<I, U> extends Executor<Maybe<Boolean>, Maybe<I>, Maybe<U>, Maybe<Integer>> {

        @NonNull
        <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                            @NonNull final Where where,
                                            @Nullable final Order order,
                                            @Nullable final Limit limit,
                                            @Nullable final Offset offset);

        interface Single<K> extends Direct<K, K> {
            interface Factory<V, K> {
                @NonNull
                Single<K> create(@NonNull final V v);
            }
        }

        interface Many<K> extends Direct<K, Integer> {
            interface Factory<V, K> {
                @NonNull
                Many<K> create(@NonNull final V v);
            }
        }
    }

    interface Async<I, U> extends Executor<Result<Boolean>, Result<I>, Result<U>, Result<Integer>> {

        @NonNull
        <M> Result<Producer<Maybe<M>>> query(@NonNull final Plan.Read<M> plan,
                                             @NonNull final Where where,
                                             @Nullable final Order order,
                                             @Nullable final Limit limit,
                                             @Nullable final Offset offset);

        interface Single<K> extends Async<K, K> {
        }

        interface Many<K> extends Async<K, Integer> {
        }
    }
}
