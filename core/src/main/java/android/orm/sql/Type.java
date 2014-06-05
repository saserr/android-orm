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

import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public interface Type<V> extends Fragment {

    @NonNls
    @NonNull
    String getName();

    @NonNls
    @NonNull
    String getWildcard();

    @NonNull
    V fromString(@NonNull final String value);

    @NonNls
    @NonNull
    String toString(@NonNull final V value);

    @NonNls
    @NonNull
    String escape(@NonNull final V value);

    @NonNull
    Maybe<V> read(@NonNull final Readable input, @NonNls @NonNull final String name);

    void write(@NonNull final Writable output,
               @NonNls @NonNull final String name,
               @NonNull final V value);

    @NonNull
    Value.Read<V> as(@NonNls @NonNull final String name);

    @NonNls
    @NonNull
    @Override
    String toSQL();

    abstract class Base<V> implements Type<V> {

        @NonNls
        @NonNull
        private final String mName;
        @NonNls
        @NonNull
        private final String mWildcard;

        protected Base(@NonNls @NonNull final String name, @NonNls @NonNull final String wildcard) {
            super();

            mName = name;
            mWildcard = wildcard;
        }

        @NonNls
        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNls
        @NonNull
        @Override
        public final String getWildcard() {
            return mWildcard;
        }

        @NonNull
        @Override
        public final Value.Read<V> as(@NonNls @NonNull final String name) {
            return Values.read(name, this);
        }

        @NonNls
        @NonNull
        @Override
        public final String toSQL() {
            return mName;
        }

        @Override
        public final boolean equals(@Nullable final Object object) {
            boolean result = false;

            if (this == object) {
                result = true;
            } else if ((object != null) && (getClass() == object.getClass())) {
                result = mName.equals(((Type<?>) object).getName());
            }

            return result;
        }

        @Override
        public final int hashCode() {
            return mName.hashCode();
        }
    }
}
