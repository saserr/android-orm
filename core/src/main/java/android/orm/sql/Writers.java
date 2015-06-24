/*
 * Copyright 2015 the original author or authors
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
import android.support.annotation.NonNull;

public final class Writers {

    @NonNull
    public static Writer compose(@NonNull final Iterable<Writer> writers) {
        return new Composition(writers);
    }

    private static class Composition implements Writer {

        @NonNull
        private final Iterable<Writer> mWriters;
        @NonNull
        private final Predicate mOnUpdate;

        private Composition(@NonNull final Iterable<Writer> writers) {
            super();

            mWriters = writers;

            Predicate onUpdate = Predicate.None;
            for (final Writer writer : writers) {
                onUpdate = onUpdate.and(writer.onUpdate());
            }
            mOnUpdate = onUpdate;
        }

        @NonNull
        @Override
        public final Predicate onUpdate() {
            return mOnUpdate;
        }

        @Override
        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final Writable output) {
            for (final Writer writer : mWriters) {
                writer.write(operation, output);
            }
        }
    }

    private Writers() {
        super();
    }
}
