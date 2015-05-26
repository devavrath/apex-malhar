/*
 * Copyright (c) 2015 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.demos.dimensions.ads.benchmark;

import com.datatorrent.api.Context;
import com.datatorrent.api.DAG;
import com.datatorrent.api.DAG.Locality;
import com.datatorrent.api.StreamingApplication;
import com.datatorrent.api.annotation.ApplicationAnnotation;
import com.datatorrent.demos.dimensions.ads.AdInfo;
import com.datatorrent.demos.dimensions.ads.AdInfo.AdInfoAggregator;
import com.datatorrent.demos.dimensions.ads.generic.InputItemGenerator;
import com.datatorrent.lib.appdata.schemas.SchemaUtils;
import com.datatorrent.lib.statistics.DimensionsComputation;
import com.datatorrent.lib.statistics.DimensionsComputationUnifierImpl;
import com.datatorrent.lib.stream.DevNull;
import org.apache.hadoop.conf.Configuration;


@ApplicationAnnotation(name="AdsDimensionsStatsBenchmark")
public class AdsDimensionsStatsBenchmark implements StreamingApplication
{
  @Override
  public void populateDAG(DAG dag, Configuration c)
  {
    InputItemGenerator input = dag.addOperator("InputGenerator", InputItemGenerator.class);
    DimensionsComputation<AdInfo, AdInfo.AdInfoAggregateEvent> dimensions = dag.addOperator("DimensionsComputation", new DimensionsComputation<AdInfo, AdInfo.AdInfoAggregateEvent>());
    dag.getMeta(dimensions).getAttributes().put(Context.OperatorContext.APPLICATION_WINDOW_COUNT, 60);
    DevNull<Object> devNull = dag.addOperator("DevNull", new DevNull<Object>());

    input.setEventSchemaJSON(SchemaUtils.jarResourceFileToString("adsBenchmarkSchema.json"));

    String[] dimensionSpecs = new String[] {
      "time=" + "1m",
      "time=" + "1m" + ":location",
      "time=" + "1m" + ":advertiser",
      "time=" + "1m" + ":publisher",
      "time=" + "1m" + ":advertiser:location",
      "time=" + "1m" + ":publisher:location",
      "time=" + "1m" + ":publisher:advertiser",
      "time=" + "1m" + ":publisher:advertiser:location"
    };

    AdInfoAggregator[] aggregators = new AdInfoAggregator[dimensionSpecs.length];
    for(int i = dimensionSpecs.length; i-- > 0;) {
      AdInfoAggregator aggregator = new AdInfoAggregator();
      aggregator.init(dimensionSpecs[i]);
      aggregators[i] = aggregator;
    }

    dimensions.setAggregators(aggregators);
    DimensionsComputationUnifierImpl<AdInfo, AdInfo.AdInfoAggregateEvent> unifier = new DimensionsComputationUnifierImpl<AdInfo, AdInfo.AdInfoAggregateEvent>();
    unifier.setAggregators(aggregators);
    dimensions.setUnifier(unifier);

    dag.addStream("InputStream", input.outputPort, dimensions.data).setLocality(Locality.CONTAINER_LOCAL);
    dag.addStream("DimensionalData", dimensions.output, devNull.data);
  }
}
