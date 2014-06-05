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

import android.content.ContentValues;
import android.support.annotation.NonNull;

import static android.orm.sql.Helper.escape;

public final class Writables {

    @NonNull
    public static Writable writable(@NonNull final ContentValues values) {
        return new ContentValuesWritable(values);
    }

    private static class ContentValuesWritable implements Writable {

        @NonNull
        private final ContentValues mValues;

        private ContentValuesWritable(@NonNull final ContentValues values) {
            super();

            mValues = values;
        }

        @Override
        public final boolean contains(@NonNull final String key) {
            return mValues.containsKey(escape(key));
        }

        @Override
        public final void putNull(@NonNull final String key) {
            mValues.putNull(escape(key));
        }

        @Override
        public final void put(@NonNull final String key, @NonNull final String value) {
            mValues.put(escape(key), value);
        }

        @Override
        public final void put(@NonNull final String key, @NonNull final Long value) {
            mValues.put(escape(key), value);
        }

        @Override
        public final void put(@NonNull final String key, @NonNull final Double value) {
            mValues.put(escape(key), value);
        }
    }

    private Writables() {
        super();
    }
}
