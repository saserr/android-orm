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

package android.orm.sql.table;

import android.orm.sql.fragment.Constraint;
import android.orm.sql.fragment.Where;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public class Check implements Constraint {

    @NonNls
    @NonNull
    private final String mExpression;
    @NonNls
    @NonNull
    private final String mSQL;

    private Check(@NonNull final String expression) {
        super();

        mExpression = expression;
        mSQL = "check (" + expression + ')';
    }

    @Override
    public final boolean equals(@Nullable final Object object) {
        boolean result = this == object;

        if (!result && (object != null) && (getClass() == object.getClass())) {
            final Check other = (Check) object;
            result = mSQL.equals(other.mSQL);
        }

        return result;
    }

    @Override
    public final int hashCode() {
        return mSQL.hashCode();
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL;
    }

    @NonNls
    @NonNull
    @Override
    public final String toString() {
        return mExpression;
    }

    @NonNull
    public static Check that(@NonNull final Where where) {
        final String expression = where.toSQL();
        if (expression == null) {
            throw new IllegalArgumentException("Where cannot be None");
        }

        return new Check(expression);
    }
}
