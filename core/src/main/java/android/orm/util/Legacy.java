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

package android.orm.util;

import android.content.ContentValues;
import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.KITKAT;

public final class Legacy {

    public static boolean equals(@Nullable final Object first, @Nullable final Object second) {
        final boolean result;

        if (SDK_INT >= KITKAT) {
            result = Objects.equals(first, second);
        } else {
            result = (first == null) ? (second == null) : first.equals(second);
        }

        return result;
    }

    @NonNull
    public static SQLException wrap(@NonNls @NonNull final String message,
                                    @NonNull final SQLException cause) {
        final SQLException exception;

        if (SDK_INT >= JELLY_BEAN) {
            exception = new SQLException(message, cause);
        } else {
            exception = cause;
        }

        return exception;
    }

    @NonNull
    public static SQLException wrap(@NonNls @NonNull final String message,
                                    @NonNull final Throwable cause) {
        final SQLException exception;

        if (SDK_INT >= JELLY_BEAN) {
            exception = new SQLException(message, cause);
        } else {
            exception = new SQLException(message);
        }

        return exception;
    }

    @NonNull
    public static <V> SparseArray<V> clone(@NonNull final SparseArray<V> original) {
        final SparseArray<V> clone;

        if (SDK_INT >= ICE_CREAM_SANDWICH) {
            clone = original.clone();
        } else {
            final int size = original.size();
            clone = new SparseArray<>(size);
            for (int i = 0; i < size; i++) {
                clone.put(original.keyAt(i), original.valueAt(i));
            }
        }

        return clone;
    }

    @NonNull
    public static Set<String> getKeys(@NonNull final ContentValues values) {
        final Set<String> keys;

        if (SDK_INT >= HONEYCOMB) {
            keys = values.keySet();
        } else {
            keys = new HashSet<>(values.size());
            for (final Map.Entry<String, Object> entry : values.valueSet()) {
                keys.add(entry.getKey());
            }
        }

        return keys;
    }

    private Legacy() {
        super();
    }
}
