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

package android.orm.database.table;

import android.orm.sql.Column;
import android.orm.sql.table.Check;
import android.orm.sql.table.ForeignKey;
import android.orm.sql.table.PrimaryKey;
import android.orm.sql.table.UniqueKey;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public interface Revision {

    @NonNull
    Revision rename(@NonNls @NonNull final String name);

    @NonNull
    Revision add(@NonNull final Column<?> column);

    @NonNull
    Revision update(@NonNull final Column<?> before, @NonNull final Column<?> after);

    @NonNull
    Revision remove(@NonNull final Column<?> column);

    @NonNull
    Revision add(@NonNull final Check check);

    @NonNull
    Revision remove(@NonNull final Check check);

    @NonNull
    Revision add(@NonNull final ForeignKey<?> foreignKey);

    @NonNull
    Revision remove(@NonNull final ForeignKey<?> foreignKey);

    @NonNull
    Revision add(@NonNull final UniqueKey<?> uniqueKey);

    @NonNull
    Revision remove(@NonNull final UniqueKey<?> uniqueKey);

    @NonNull
    Revision with(@NonNull final PrimaryKey<?> primaryKey);

    @NonNull
    Revision withoutPrimaryKey();
}
