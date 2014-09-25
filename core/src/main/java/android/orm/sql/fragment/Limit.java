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

package android.orm.sql.fragment;

import android.orm.sql.Fragment;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public class Limit implements Fragment {

    public static final Limit Single = new Limit(1);

    private final int mAmount;
    @NonNls
    @NonNull
    private final String mSQL;

    public Limit(final int amount) {
        super();

        if (amount < 0) {
            throw new IllegalArgumentException("Limit must be non-negative number");
        }

        mAmount = amount;
        mSQL = String.valueOf(amount);
    }

    public final int getAmount() {
        return mAmount;
    }

    @NonNls
    @NonNull
    @Override
    public final String toSQL() {
        return mSQL;
    }

    @NonNull
    public static Limit limit(final int amount) {
        return new Limit(amount);
    }
}
