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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import static android.orm.util.Maybes.something;

public class Exists extends Function.Base<Select.Where, Maybe<Boolean>> {

    private static final String[] PROJECTION = {"1"};

    @NonNull
    private final ContentResolver mResolver;
    @NonNull
    private final Uri mUri;

    public Exists(@NonNull final ContentResolver resolver, @NonNull final Uri uri) {
        super();

        mResolver = resolver;
        mUri = uri;
    }

    @NonNull
    @Override
    public final Maybe<Boolean> invoke(@NonNull final Select.Where where) {
        final Maybe<Boolean> result;

        Cursor cursor = null;
        try {
            cursor = mResolver.query(mUri, PROJECTION, where.toSQL(), null, null);
            result = something((cursor != null) && (cursor.getCount() > 0));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }
}
