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

import android.orm.sql.Reader;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public interface Executor<E, I, U, D> {

    @NonNull
    E exists(@NonNull final Predicate predicate);

    @NonNull
    I insert(@NonNull final Writer writer);

    @NonNull
    U update(@NonNull final Predicate predicate, @NonNull final Writer writer);

    @NonNull
    D delete(@NonNull final Predicate predicate);

    interface Direct<I, U> extends Executor<Maybe<Boolean>, Maybe<I>, Maybe<U>, Maybe<Integer>> {

        @NonNull
        <M> Maybe<Producer<Maybe<M>>> query(@NonNull final Reader.Collection<M> reader,
                                            @NonNull final Predicate predicate,
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
        <M> Result<Producer<Maybe<M>>> query(@NonNull final Reader.Collection<M> reader,
                                             @NonNull final Predicate predicate,
                                             @Nullable final Order order,
                                             @Nullable final Limit limit,
                                             @Nullable final Offset offset);

        interface Single<K> extends Async<K, K> {
        }

        interface Many<K> extends Async<K, Integer> {
        }
    }
}
