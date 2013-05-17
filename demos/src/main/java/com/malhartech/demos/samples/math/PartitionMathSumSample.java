package com.malhartech.demos.samples.math;

import org.apache.hadoop.conf.Configuration;

import com.malhartech.api.ApplicationFactory;
import com.malhartech.api.Context.OperatorContext;
import com.malhartech.api.DAG;
import com.malhartech.lib.io.ConsoleOutputOperator;
import com.malhartech.lib.math.Sum;
import com.malhartech.lib.testbench.RandomEventGenerator;

/**
 *  * This sample application code for showing sample usage of malhar operator(s). <br>
 * <b>Operator : </b> Sum <br>
 * <bClass : </b> com.malhartech.lib.math.Sum 
 * Sum operator is partitioned into 4 operator, partitioning is allowed on this operator. <br>
 * 
 * @author Dinesh Prasad(dinesh@malhar-inc.com)
 */
public class PartitionMathSumSample implements ApplicationFactory
{
	@SuppressWarnings("unchecked")
	@Override
	public DAG getApplication(Configuration conf)
	{
		// Create application dag.
		DAG dag = new DAG(conf);
		dag.setAttribute(DAG.STRAM_APPNAME, "MobileDevApplication");
		dag.setAttribute(DAG.STRAM_DEBUG, true);

		// Add random integer generator operator
		RandomEventGenerator rand = dag.addOperator("rand",
				RandomEventGenerator.class);
		rand.setMaxvalue(1000);
		rand.setTuplesBlast(10);
		rand.setTuplesBlastIntervalMillis(500);

		Sum<Integer> sum = (Sum<Integer>) dag.addOperator("sum", Sum.class);
		dag.addStream("stream1", rand.integer_data, sum.data);
		dag.getMeta(sum).getAttributes()
				.attr(OperatorContext.INITIAL_PARTITION_COUNT).set(4);
		dag.getMeta(sum).getAttributes()
				.attr(OperatorContext.APPLICATION_WINDOW_COUNT).set(20);

		// Connect to output console operator
		ConsoleOutputOperator console = dag.addOperator("console",
				new ConsoleOutputOperator());
		dag.addStream("stream2", sum.sum, console.input);

		// done
		return dag;
	}

}
