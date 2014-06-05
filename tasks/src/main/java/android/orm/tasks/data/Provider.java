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

import android.orm.BaseContentProvider;
import android.orm.Database;
import android.orm.Route;
import android.orm.database.IntegrityChecks;
import android.orm.sql.Table;
import android.orm.tasks.BuildConfig;
import android.orm.tasks.model.Task;

import static android.orm.sql.Table.table;
import static android.orm.sql.statement.Select.Order.Type.Ascending;
import static android.orm.sql.statement.Select.order;

public class Provider extends BaseContentProvider {

    private static final int VERSION = 2;
    private static final Route.Manager ROUTES = new Route.Manager(BuildConfig.PACKAGE_NAME, "vnd.orm");
    private static final Database DATABASE = new Database("tasks.db", VERSION, IntegrityChecks.Full)
            .migrate(Tables.Tasks);

    public interface Tables {
        Table Tasks = table("tasks", 1)
                .with(Task.Id)
                .with(Task.Title)
                .with(2, Task.Finished)
                .with(order(Task.Id, Ascending));
    }

    public interface Routes {
        Route.Item TaskById = ROUTES.item(Tables.Tasks);
        Route.Dir Tasks = ROUTES.dir(TaskById, Tables.Tasks);
    }

    public Provider() {
        super(DATABASE, ROUTES);
    }
}
