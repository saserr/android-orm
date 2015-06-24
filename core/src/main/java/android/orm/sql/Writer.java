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

import android.orm.sql.fragment.Predicate;
import android.orm.util.Function;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public interface Writer {

    @NonNull
    Predicate onUpdate();

    void write(@NonNull final Value.Write.Operation operation, @NonNull final Writable output);

    Writer Empty = new Writer() {

        @NonNull
        @Override
        public Predicate onUpdate() {
            return Predicate.None;
        }

        @Override
        public void write(@NonNull final Value.Write.Operation operation,
                          @NonNull final Writable output) {/* do nothing */}
    };

    class Builder<V> {

        @NonNull
        private final Collection<Writer> mWriters;
        @NonNull
        private final Collection<Function<V, Writer>> mFactories;

        public Builder() {
            super();

            mWriters = new LinkedList<>();
            mFactories = new LinkedList<>();
        }

        public Builder(@NonNull final Builder<V> builder) {
            super();

            mWriters = new LinkedList<>(builder.mWriters);
            mFactories = new LinkedList<>(builder.mFactories);
        }

        @NonNull
        public final Builder<V> with(@NonNull final Writer writer) {
            mWriters.add(writer);
            return this;
        }

        @NonNull
        public final Builder<V> with(@NonNull final Function<V, Writer> factory) {
            mFactories.add(factory);
            return this;
        }

        @NonNull
        public final Writer build(@NonNull final V value) {
            final Collection<Writer> writers = new ArrayList<>(mWriters.size() + mFactories.size());

            writers.addAll(mWriters);
            for (final Function<V, Writer> factories : mFactories) {
                writers.add(factories.invoke(value));
            }

            return Writers.compose(writers);
        }
    }
}
