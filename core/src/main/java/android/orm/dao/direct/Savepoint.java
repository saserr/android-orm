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

package android.orm.dao.direct;

import android.database.sqlite.SQLiteDatabase;
import android.orm.sql.Statement;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public abstract class Savepoint implements Statement {

    @NonNls
    @NonNull
    private final String mName;

    protected Savepoint(@NonNls @NonNull final String name) {
        super();

        mName = name;
    }

    public abstract void rollback();

    @Override
    public final void execute(@NonNull final SQLiteDatabase database) {
        database.execSQL("savepoint " + mName + ';'); //NON-NLS
    }

    protected final void rollback(@NonNull final SQLiteDatabase database) {
        database.execSQL("rollback transaction to savepoint " + mName + ';'); //NON-NLS
    }
}
