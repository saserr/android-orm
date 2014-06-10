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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.util.Maybes.something;

public abstract class Action<V, T> {

    private static final Rollback Rollback = new Rollback();

    @NonNull
    public abstract Statement<T> onResult(@NonNull final DAO dao, @NonNull final Maybe<V> v);

    @NonNull
    public final Statement<T> value(@Nullable final T value) {
        return value(something(value));
    }

    @NonNull
    public final Statement<T> value(@NonNull final Maybe<T> value) {
        return new Statement<>(Functions.<SQLiteDatabase, Maybe<T>>singleton(value));
    }

    @NonNull
    public final Statement<T> rollback() {
        throw Rollback;
    }

    public static class Rollback extends SQLException {

        private static final long serialVersionUID = -7085222963689272905L;

        private Rollback() {
            super("Rollback");
        }
    }
}
