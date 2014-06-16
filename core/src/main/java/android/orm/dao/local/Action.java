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

package android.orm.dao.local;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.something;

public abstract class Action<V, T> {

    private static final Abort Abort = new Abort();

    @NonNull
    public abstract Statement<T> onResult(@NonNull final DAO dao, @NonNull final Maybe<V> v);

    @NonNull
    public final Statement<T> rollback() {
        throw Abort;
    }

    @NonNull
    public final Statement<T> rollback(@NonNls @NonNull final String savepoint) {
        return rollback(savepoint, Maybes.<T>nothing());
    }

    @NonNull
    public final Statement<T> rollback(@NonNls @NonNull final String savepoint,
                                       @NonNull final T result) {
        return rollback(savepoint, something(result));
    }

    @NonNull
    public final Statement<T> rollback(@NonNls @NonNull final String savepoint,
                                       @NonNull final Maybe<T> result) {
        return new Statement<>(new Rollback<>(savepoint, result));
    }

    @NonNull
    public static <U> Statement<U> value(@Nullable final U value) {
        return value(something(value));
    }

    @NonNull
    public static <U> Statement<U> value(@NonNull final Maybe<U> value) {
        return new Statement<>(Functions.<SQLiteDatabase, Maybe<U>>singleton(value));
    }

    public static class Abort extends SQLException {

        private static final long serialVersionUID = -703635940178327094L;

        private Abort() {
            super("Abort");
        }
    }
}
