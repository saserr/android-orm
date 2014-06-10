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

package android.orm.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

public interface Maybe<V> {

    boolean isSomething();

    boolean isNothing();

    @Nullable
    V get();

    @NonNull
    <T> Maybe<T> map(@NonNull final Function<? super V, ? extends T> function);

    @NonNull
    <T> Maybe<T> flatMap(@NonNull final Function<? super V, Maybe<T>> function);

    @NonNull
    <T> Maybe<Pair<V, T>> and(@NonNull final Maybe<? extends T> other);

    @NonNull
    <T extends V> Maybe<V> or(@NonNull final Maybe<T> other);

    @Nullable
    <T extends V> V getOrElse(@Nullable final T other);
}
