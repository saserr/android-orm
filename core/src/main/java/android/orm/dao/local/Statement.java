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

import android.database.sqlite.SQLiteDatabase;
import android.orm.util.Function;
import android.orm.util.Functions;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;

public class Statement<V> {

    @NonNull
    private final Function<SQLiteDatabase, Maybe<V>> mFunction;

    public Statement(@NonNull final Function<SQLiteDatabase, Maybe<V>> function) {
        super();

        mFunction = function;
    }

    @NonNull
    public final <T> Statement<T> map(@NonNull final Function<? super V, ? extends T> converter) {
        return new Statement<>(mFunction.compose(Maybes.map(converter)));
    }

    @NonNull
    public final <T> Statement<T> flatMap(@NonNull final Function<? super V, Maybe<T>> converter) {
        return new Statement<>(mFunction.compose(Maybes.flatMap(converter)));
    }

    @NonNull
    public final <T> Statement<T> convert(@NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new Statement<>(mFunction.compose(converter));
    }

    @NonNull
    public final <T> Statement<Pair<V, T>> and(@NonNull final Maybe<T> other) {
        return new Statement<>(mFunction.and(Functions.singleton(other)).compose(Maybes.<V, T>liftPair()));
    }

    @NonNull
    public final <T> Statement<Pair<V, T>> and(@NonNull final Statement<T> other) {
        return new Statement<>(mFunction.and(other.mFunction).compose(Maybes.<V, T>liftPair()));
    }

    @NonNull
    public final Maybe<V> execute(@NonNull final SQLiteDatabase database) {
        return mFunction.invoke(database);
    }
}
