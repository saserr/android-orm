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

public interface ConflictResolution extends Fragment {

    ConflictResolution Rollback = new ConflictResolution() {
        @NonNls
        @NonNull
        @Override
        public String toSQL() {
            return "roolback";
        }
    };

    ConflictResolution Abort = new ConflictResolution() {
        @NonNls
        @NonNull
        @Override
        public String toSQL() {
            return "abort";
        }
    };

    ConflictResolution Fail = new ConflictResolution() {
        @NonNls
        @NonNull
        @Override
        public String toSQL() {
            return "fail";
        }
    };

    ConflictResolution Ignore = new ConflictResolution() {
        @NonNls
        @NonNull
        @Override
        public String toSQL() {
            return "ignore";
        }
    };

    ConflictResolution Replace = new ConflictResolution() {
        @NonNls
        @NonNull
        @Override
        public String toSQL() {
            return "replace";
        }
    };
}
