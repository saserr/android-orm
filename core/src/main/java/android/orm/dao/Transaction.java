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
import android.orm.dao.direct.Savepoint;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NonNls;

public final class Transaction {

    public interface Direct extends DAO.Direct {
        @NonNull
        Savepoint savepoint(@NonNls @NonNull final String name);
    }

    public interface Local<V> {

        @NonNull
        Maybe<V> run(@NonNull final Direct transaction) throws Rollback;

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

        private static final long serialVersionUID = -3964626269111732824L;

        public Rollback(@NonNls @NonNull final String message) {
            super(message);
        }
    }

    private Transaction() {
        super();
    }
}
