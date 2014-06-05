/*
 * Copyright 2013 the original author or authors
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

package android.orm.dao.operation;

import android.net.Uri;
import android.orm.Route;
import android.orm.sql.Readable;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import static android.orm.sql.Readables.readable;
import static android.orm.util.Maybes.something;

public class Parse extends Function.Base<Maybe<Uri>, Maybe<Readable>> {

    @NonNull
    private final Route.Item mRoute;

    public Parse(@NonNull final Route.Item route) {
        super();

        mRoute = route;
    }

    @NonNull
    @Override
    public final Maybe<Readable> invoke(@NonNull final Maybe<Uri> result) {
        final Uri uri = result.getOrElse(null);
        return (uri == null) ?
                Maybes.<Readable>nothing() :
                something(readable(mRoute.parse(uri)));
    }
}
