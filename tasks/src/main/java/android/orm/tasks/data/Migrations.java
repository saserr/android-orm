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

package android.orm.tasks.data;

import android.orm.database.Table;
import android.orm.tasks.model.Task;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.PrimaryKey.primaryKey;

public final class Migrations {

    @NonNls
    public static final Table.Migration Tasks;

    /* tasks table migrations */
    static {
        Tasks = new Table.Migration("tasks");
        Tasks.at(1)
                .add(Task.Id)
                .add(Task.Title)
                .with(primaryKey(Task.Id));
        Tasks.at(2)
                .add(Task.Finished);
        Tasks.at(3)
                .add(Task.Version);
    }

    private Migrations() {
        super();
    }
}
