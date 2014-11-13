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

import android.orm.database.Table;
import android.orm.sql.Column;
import android.orm.sql.Statement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public interface Schema {

    void rename(@NonNls @NonNull final String name);

    void update(@Nullable final Column<?> before, @Nullable final Column<?> after);

    void update(@Nullable final Check before, @Nullable final Check after);

    void update(@Nullable final ForeignKey<?> before, @Nullable final ForeignKey<?> after);

    void with(@Nullable final PrimaryKey<?> primaryKey);

    @NonNull
    Table<?> table();

    @NonNull
    Statement statement(final int version);
}
