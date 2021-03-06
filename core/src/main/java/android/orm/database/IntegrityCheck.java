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

package android.orm.database;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

public interface IntegrityCheck {

    void check(@NonNull final SQLiteDatabase database);

    @NonNull
    IntegrityCheck and(@NonNull final IntegrityCheck other);

    abstract class Base implements IntegrityCheck {
        @NonNull
        @Override
        public final IntegrityCheck and(@NonNull final IntegrityCheck other) {
            return IntegrityChecks.compose(this, other);
        }
    }
}
