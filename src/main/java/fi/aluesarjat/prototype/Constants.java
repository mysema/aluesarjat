/*
* Copyright 2013 Mysema Ltd
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fi.aluesarjat.prototype;

import com.mysema.rdfbean.model.QLIT;
import com.mysema.rdfbean.model.QUID;

public final class Constants {

    public static final QUID dataset = new QUID("dataset");

    public static final QUID dimensionType = new QUID("dimensionType");

    public static final QLIT dimensionTypeName = new QLIT("dimensionTypeName");

    public static final QUID dimension = new QUID("dimension");

    public static final QUID dimensionProperty = new QUID("dimensionProperty");

    public static final QLIT dimensionDescription = new QLIT("dimensionDescription");

    public static final QLIT dimensionName = new QLIT("dimensionName");

    public static final QLIT value = new QLIT("value");

    public static final QUID item = new QUID("item");

    public static final QUID parent = new QUID("parent");

    private Constants() {}

}
