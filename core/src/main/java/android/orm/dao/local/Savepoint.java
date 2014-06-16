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
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Savepoint<V> implements Function<SQLiteDatabase, Maybe<V>> {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Maybe<V> mResult;

    public Savepoint(@NonNls @NonNull final String name, @NonNull final Maybe<V> result) {
        super();

        mName = name;
        mResult = result;
    }

    @NonNull
    @Override
    public final Maybe<V> invoke(@NonNull final SQLiteDatabase database) {
        database.execSQL("savepoint " + mName + ';'); //NON-NLS
        return mResult;
    }
}
