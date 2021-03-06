/*
 * Copyright 2011 John Yeary <jyeary@bluelotussoftware.com>.
 * Copyright 2011 Bluelotus Software, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.batch.rest.poc.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericMapType<K, V> {

    private final List<GenericMapEntryType<K, V>> entry = new ArrayList<>();

    public GenericMapType() {
    }

    public GenericMapType(final Map<K, V> map) {
        map.entrySet().forEach(e -> entry.add(new GenericMapEntryType<>(e)));
    }

    public List<GenericMapEntryType<K, V>> getEntry() {
        return entry;
    }

    public void setEntry(final List<GenericMapEntryType<K, V>> entry) {
        this.entry.clear();
        if (entry != null) {
            this.entry.addAll(entry);
        }
    }
}
