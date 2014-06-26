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

package android.orm.dao;

import android.content.ContentProviderResult;
import android.orm.DAO;
import android.orm.Route;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public final class Transaction {

    public interface Direct<V> {

        @NonNull
        Maybe<V> run(@NonNull final DAO.Direct dao) throws Rollback;

        Rollback Rollback = new Rollback("Rollback");
    }

    public interface Remote {

        @NonNull
        Access at(@NonNull final Route route, @NonNull final Object... arguments);

        @NonNull
        Result<CommitResult> commit();

        interface Access extends DAO.Access.Write<Access, Access, Access> {
        }

        interface CommitResult {

            @NonNls
            @NonNull
            String getAuthority();

            @NonNull
            ContentProviderResult[] getResults();
        }
    }

    public static class Rollback extends Exception {

        private static final long serialVersionUID = 4413464485439328954L;

        private Rollback(@NonNls @NonNull final String error) {
            super(error);
        }
    }

    private Transaction() {
        super();
    }
}
