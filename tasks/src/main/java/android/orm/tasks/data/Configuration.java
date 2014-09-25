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

import android.orm.Database;
import android.orm.database.IntegrityChecks;
import android.orm.sql.Table;
import android.orm.tasks.model.Task;

import static android.orm.sql.PrimaryKey.primaryKey;
import static android.orm.sql.Table.table;
import static android.orm.sql.fragment.Order.Type.Ascending;
import static android.orm.sql.fragment.Order.order;

public final class Configuration {

    private static final int VERSION = 2;

    public static final Database DATABASE = new Database("tasks.db", VERSION, IntegrityChecks.Full)
            .migrate(Tables.Tasks);

    public interface Tables {
        Table<Long> Tasks = table("tasks", 1)
                .with(Task.Id)
                .with(Task.Title)
                .with(2, Task.Finished)
                .with(primaryKey(Task.Id))
                .with(order(Task.Id, Ascending));
    }

    private Configuration() {
        super();
    }
}
