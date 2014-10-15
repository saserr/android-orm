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

package android.orm.sql;

import android.orm.util.Converter;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public interface Type<V> extends Fragment {

    @NonNull
    Primitive getPrimitive();

    @NonNull
    V fromString(@NonNls @NonNull final String value);

    @NonNls
    @NonNull
    String toString(@NonNull final V value);

    @NonNls
    @NonNull
    String escape(@NonNull final V value);

    @NonNull
    Maybe<V> read(@NonNull final Readable input, @NonNls @NonNull final String name);

    @NonNull
    <T> Type<T> map(@NonNull final Converter<T, V> converter);

    void write(@NonNull final Writable output,
               @NonNls @NonNull final String name,
               @NonNull final V value);

    @NonNull
    Value.ReadWrite<V> as(@NonNls @NonNull final String name);

    @NonNls
    @NonNull
    @Override
    String toSQL();

    abstract class Base<V> implements Type<V> {

        @NonNls
        @NonNull
        private final Primitive mPrimitive;

        protected Base(@NonNls @NonNull final Primitive primitive) {
            super();

            mPrimitive = primitive;
        }

        @NonNull
        @Override
        public final Primitive getPrimitive() {
            return mPrimitive;
        }

        @NonNull
        @Override
        public final <T> Type<T> map(@NonNull final Converter<T, V> converter) {
            return Types.map(this, converter);
        }

        @NonNull
        @Override
        public final Value.ReadWrite<V> as(@NonNls @NonNull final String name) {
            return Values.value(name, this);
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mPrimitive.toSQL();
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = false;

            if (this == object) {
                result = true;
            } else if ((object != null) && (getClass() == object.getClass())) {
                result = mPrimitive == ((Type<?>) object).getPrimitive();
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return mPrimitive.hashCode();
        }
    }

    enum Primitive implements Fragment {

        Text("text"),
        Integer("integer"),
        Real("real");

        @NonNls
        @NonNull
        private final String mSQL;

        Primitive(@NonNls @NonNull final String sql) {
            mSQL = sql;
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mSQL;
        }
    }
}
