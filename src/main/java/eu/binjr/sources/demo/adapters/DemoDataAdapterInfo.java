
/*
 *    Copyright 2019-2020 Frederic Thevenet
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package eu.binjr.sources.demo.adapters;


import eu.binjr.core.data.adapters.AdapterMetadata;
import eu.binjr.core.data.adapters.BaseDataAdapterInfo;
import eu.binjr.core.data.adapters.SourceLocality;
import eu.binjr.core.data.exceptions.CannotInitializeDataAdapterException;

/**
 * Defines the metadata associated with {@link DemoDataAdapter}.
 *
 * @author Frederic Thevenet
 */
@AdapterMetadata(
        name = "Demo",
        description = "Demo Data Adapter",
        copyright = "Copyright © 2019-2020 Frederic Thevenet",
        license = "Apache-2.0",
        siteUrl = "https://github.com/binjr/binjr-adapter-demo",
        adapterClass = DemoDataAdapter.class,
        dialogClass = DemoDataAdapterDialog.class,
        sourceLocality = SourceLocality.LOCAL,
        apiLevel = "3.0.0")
public class DemoDataAdapterInfo extends BaseDataAdapterInfo {

    /**
     * Initialises a new instance of the {@link DemoDataAdapterInfo} class.
     */
    public DemoDataAdapterInfo() throws CannotInitializeDataAdapterException {
        super(DemoDataAdapterInfo.class);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
