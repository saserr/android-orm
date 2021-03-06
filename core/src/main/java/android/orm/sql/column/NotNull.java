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

package android.orm.sql.column;

import android.orm.sql.fragment.ConflictResolution;
import android.orm.sql.fragment.Constraint;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class NotNull implements Constraint {

    @NonNls
    @NonNull
    private final String mSQL;

    public NotNull() {
        super();

        mSQL = "not null";
    }

    public NotNull(@NonNull final ConflictResolution onConflict) {
        super();

        mSQL = "not null on conflict " + onConflict.toSQL();
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL;
    }
}
