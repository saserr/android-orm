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

import android.orm.sql.Fragment;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public enum ConflictResolution implements Fragment {

    Rollback("rollback"),
    Abort("abort"),
    Fail("fail"),
    Ignore("ignore"),
    Replace("replace");

    @NonNls
    @NonNull
    private final String mSQL;

    ConflictResolution(@NonNls @NonNull final String sql) {
        mSQL = sql;
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL;
    }
}
