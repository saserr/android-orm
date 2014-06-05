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

import static android.orm.util.Converters.from;
import static android.orm.util.Converters.to;
import static android.orm.util.Lenses.combine;

public final class Lens {

    public interface Read<M, V> {

        @Nullable
        V get(@NonNull final M m);

        @NonNull
        Read<M, V> checkThat(@NonNull final Validation<? super V> validation);

        @NonNull
        <T> Read<M, T> mapTo(@NonNull final Function<? super V, T> converter);

        abstract class Base<M, V> implements Read<M, V> {

            @NonNull
            @Override
            public final Read<M, V> checkThat(@NonNull final Validation<? super V> validation) {
                return Lenses.check(this, validation);
            }

            @NonNull
            @Override
            public final <T> Read<M, T> mapTo(@NonNull final Function<? super V, T> converter) {
                return Lenses.convert(this, converter);
            }
        }
    }

    public interface Write<M, V> {

        void set(@NonNull final M m, @Nullable final V v);

        @NonNull
        <T> Write<M, T> mapFrom(@NonNull final Function<? super T, ? extends V> converter);

        abstract class Base<M, V> implements Write<M, V> {

            @NonNull
            @Override
            public final <T> Write<M, T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Lenses.convert(this, converter);
            }
        }
    }

    public interface ReadWrite<M, V> extends Read<M, V>, Write<M, V> {

        @NonNull
        @Override
        ReadWrite<M, V> checkThat(@NonNull final Validation<? super V> validation);

        @NonNull
        <T> ReadWrite<M, T> map(@NonNull final Converter<V, T> converter);

        abstract class Base<M, V> implements ReadWrite<M, V> {

            @NonNull
            @Override
            public final ReadWrite<M, V> checkThat(@NonNull final Validation<? super V> validation) {
                return Lenses.check(this, validation);
            }

            @NonNull
            @Override
            public final <T> Read<M, T> mapTo(@NonNull final Function<? super V, T> converter) {
                return Lenses.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> Write<M, T> mapFrom(@NonNull final Function<? super T, ? extends V> converter) {
                return Lenses.convert(this, converter);
            }

            @NonNull
            @Override
            public final <T> ReadWrite<M, T> map(@NonNull final Converter<V, T> converter) {
                return combine(mapTo(from(converter)), mapFrom(to(converter)));
            }
        }
    }

    private Lens() {
        super();
    }
}
