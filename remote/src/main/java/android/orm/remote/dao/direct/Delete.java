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

package android.orm.remote.dao.direct;

import android.content.ContentResolver;
import android.net.Uri;
import android.orm.sql.fragment.Condition;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public class Delete implements Function<Condition, Maybe<Integer>> {

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Delete(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Integer> invoke(@NonNull final Condition condition) {
        final int deleted = mResolver.delete(mUri, condition.toSQL(), null);
        return (deleted > 0) ? something(deleted) : Maybes.<Integer>nothing();
    }
}
