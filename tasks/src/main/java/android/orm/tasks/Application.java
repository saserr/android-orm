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

package android.orm.tasks;

import android.orm.Reactive;
import android.orm.tasks.data.Configuration;
import android.support.annotation.NonNull;

public class Application extends android.app.Application {

    @NonNull
    private Reactive.Async mDAO;

    @NonNull
    public final Reactive.Async getDAO() {
        return mDAO;
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        mDAO = Reactive.create(getApplicationContext(), Configuration.DATABASE);
    }
}
