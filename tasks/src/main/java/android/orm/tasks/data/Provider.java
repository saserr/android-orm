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

import android.orm.remote.ContentProvider;
import android.orm.remote.Route;
import android.orm.tasks.BuildConfig;
import android.orm.tasks.model.Task;

import static android.orm.sql.fragment.Order.Type.Ascending;
import static android.orm.sql.fragment.Order.order;
import static android.orm.tasks.data.Configuration.Database;

public class Provider extends ContentProvider {

    private static final Route.Manager ROUTES = new Route.Manager(BuildConfig.APPLICATION_ID);

    public interface Routes {
        Route.Single TaskById = ROUTES.single(Tables.Tasks.getName(), Task.Id);
        Route.Many Tasks = ROUTES.many(TaskById, order(Task.Id, Ascending));
    }

    public Provider() {
        super(Database, "orm", ROUTES);
    }
}
