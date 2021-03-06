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

package android.orm.playground.reactive;

import android.orm.playground.Reactive;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;

public final class Transaction {

    public interface Direct<V> {

        @NonNull
        Maybe<V> run(@NonNull final Reactive.Direct dao) throws android.orm.dao.Transaction.Rollback;

        android.orm.dao.Transaction.Rollback Rollback = android.orm.dao.Transaction.Direct.Rollback;
    }

    private Transaction() {
        super();
    }
}
