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

package android.orm.database;

import android.support.annotation.NonNull;

public interface Migration {

    void create(@NonNull final DAO dao, final int version);

    void upgrade(@NonNull final DAO dao, final int oldVersion, final int newVersion);

    void downgrade(@NonNull final DAO dao, final int oldVersion, final int newVersion);

    abstract class Base implements Migration {

        @Override
        public void create(@NonNull final DAO dao, final int version) {
            /* do nothing */
        }

        @Override
        public void upgrade(@NonNull final DAO dao, final int oldVersion, final int newVersion) {
            /* do nothing */
        }

        @Override
        public void downgrade(@NonNull final DAO dao, final int oldVersion, final int newVersion) {
            /* do nothing */
        }
    }

    abstract class StepWise extends Base {

        protected abstract void upgrade(@NonNull final DAO dao, final int version);

        protected abstract void downgrade(@NonNull final DAO dao, final int version);

        @Override
        public final void upgrade(@NonNull final DAO dao,
                                  final int oldVersion,
                                  final int newVersion) {
            for (int version = oldVersion + 1; version <= newVersion; version++) {
                upgrade(dao, version);
            }
        }

        @Override
        public final void downgrade(@NonNull final DAO dao,
                                    final int oldVersion,
                                    final int newVersion) {
            for (int version = oldVersion; version > newVersion; version--) {
                downgrade(dao, version);
            }
        }
    }
}
