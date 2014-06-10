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
import android.util.Pair;

public interface Producer<V> {

    @NonNull
    V produce();

    @NonNull
    <T> Producer<T> map(@NonNull final Function<? super V, ? extends T> function);

    @NonNull
    <T> Producer<Pair<V, T>> and(@NonNull final Producer<? extends T> other);

    abstract class Base<V> implements Producer<V> {

        @NonNull
        @Override
        public final <T> Producer<T> map(@NonNull final Function<? super V, ? extends T> function) {
            return Producers.convert(this, function);
        }

        @NonNull
        @Override
        public final <T> Producer<Pair<V, T>> and(@NonNull final Producer<? extends T> other) {
            return Producers.compose(this, other);
        }
    }
}
