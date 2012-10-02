/**
 * Copyright (c) 2012-2012 Malhar, Inc. All rights reserved.
 */
package com.malhartech.lib.testbench;

import com.malhartech.dag.ModuleConfiguration;
import com.malhartech.dag.ModuleContext;
import com.malhartech.dag.Sink;
import com.malhartech.dag.Tuple;
import com.malhartech.stream.StramTestSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Functional test for {@link com.malhartech.lib.testbench.FilterClassifier} for three configuration><p>
 * <br>
 * Configuration 1: Provide values and weights<br>
 * Configuration 2: Provide values but no weights (even weights)<br>
 * Configuration 3: Provide no values or weights<br>
 * <br>
 * Benchmarks: Currently handle about 20 Million tuples/sec incoming tuples in debugging environment. Need to test on larger nodes<br>
 * <br>
 * Validates all DRC checks of the node<br>
 */
public class TestLoadIncrementer {

    private static Logger LOG = LoggerFactory.getLogger(FilterClassifier.class);

    class TestSink implements Sink {

        HashMap<String, String> collectedTuples = new HashMap<String, String>();
        long count = 0;

        /**
         *
         * @param payload
         */
        @Override
        public void process(Object payload) {
          if (payload instanceof Tuple) {
            // LOG.debug(payload.toString());
          }
          else {
            count++;
          }
        }

        public void clear() {
            count = 0;
            collectedTuples.clear();
        }
    }

    /**
     * Test configuration and parameter validation of the node
     */
    @Test
    public void testNodeValidation() {

        ModuleConfiguration conf = new ModuleConfiguration("mynode", new HashMap<String, String>());
        LoadIncrementer node = new LoadIncrementer();

        conf.set(FilterClassifier.KEY_KEYS, "");
        try {
            node.myValidation(conf);
            Assert.fail("validation error  " + LoadIncrementer.KEY_KEYS);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("validate " + LoadIncrementer.KEY_KEYS,
                    e.getMessage().contains("is empty"));
        }

        conf.set(LoadIncrementer.KEY_KEYS, "a,b"); // from now on keys are a,b,c
        conf.set(LoadIncrementer.KEY_LIMITS, "1,100;1,100;1,100");
        try {
            node.myValidation(conf);
            Assert.fail("validation error  " + LoadIncrementer.KEY_LIMITS);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("validate " + LoadIncrementer.KEY_LIMITS,
                    e.getMessage().contains("does not match number ids in limits"));
        }

        conf.set(LoadIncrementer.KEY_LIMITS, "1,100,200;1,100");
        try {
            node.myValidation(conf);
            Assert.fail("validation error  " + LoadIncrementer.KEY_LIMITS);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("validate " + LoadIncrementer.KEY_LIMITS,
                    e.getMessage().contains("Property \"limits\" has a illegal value"));
        }

        conf.set(LoadIncrementer.KEY_LIMITS, "1,a;1,100");
        try {
            node.myValidation(conf);
            Assert.fail("validation error  " + LoadIncrementer.KEY_LIMITS);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("validate " + LoadIncrementer.KEY_LIMITS,
                    e.getMessage().contains("has illegal format for one of its strings"));
        }

        conf.set(LoadIncrementer.KEY_LIMITS, "100,1;1,100");
        try {
            node.myValidation(conf);
            Assert.fail("validation error  " + LoadIncrementer.KEY_LIMITS);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("validate " + LoadIncrementer.KEY_LIMITS,
                    e.getMessage().contains(">= high_value"));
        }

    }

    /**
     * Test node logic emits correct results
     */
    @Test
  public void testNodeProcessing() throws Exception
  {
    final LoadIncrementer node = new LoadIncrementer();

    TestSink dataSink = new TestSink();
    TestSink countSink = new TestSink();

    node.connect(LoadIncrementer.OPORT_DATA, dataSink);
    node.connect(LoadIncrementer.OPORT_COUNT, countSink);

    Sink seedSink = node.connect(LoadIncrementer.IPORT_SEED, node);
    Sink incrSink = node.connect(LoadIncrementer.IPORT_INCREMENT, node);

    ModuleConfiguration conf = new ModuleConfiguration("mynode", new HashMap<String, String>());

    conf.set(LoadIncrementer.KEY_KEYS, "x,y");
    conf.set(LoadIncrementer.KEY_LIMITS, "1,100;1,200");
    conf.set(LoadIncrementer.KEY_DELTA , "1");
    node.setup(conf);

    final AtomicBoolean inactive = new AtomicBoolean(true);
    new Thread()
    {
      @Override
      public void run()
      {
        inactive.set(false);
        node.activate(new ModuleContext("LoadIncrementerTestNode", this));
      }
    }.start();

    // spin while the node gets activated./
    int sleeptimes = 0;
    try {
      do {
        Thread.sleep(20);
        sleeptimes++;
        if (sleeptimes > 5) {
          break;
        }
      }
      while (inactive.get());
    }
    catch (InterruptedException ex) {
      LOG.debug(ex.getLocalizedMessage());
    }

    Tuple bt = StramTestSupport.generateBeginWindowTuple("doesn't matter", 1);
    seedSink.process(bt);
    incrSink.process(bt);

    HashMap<String, Object> input = new HashMap<String, Object>();
    int sentval = 0;
    int numtuples = 10000000;
    String seed1 = "a";
    ArrayList val = new ArrayList();
    val.add(new Integer(10));
    val.add(new Integer(20));

    for (int i = 0; i < numtuples; i++) {
      input = new HashMap<String, Object>();
      input.put(seed1, val);
      seedSink.process(input);
    }

    Tuple et = StramTestSupport.generateEndWindowTuple("doesn't matter", 1, 1);
    seedSink.process(et);
    incrSink.process(et);
    // Let the reciever get the tuples from the queue
    for (int i = 0; i < 20; i++) {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException e) {
        LOG.error("Unexpected error while sleeping for 1 s", e);
      }
    }

    LOG.debug(String.format("\n*************************\nEmitted %d tuples, Processed %d tuples, Received %d tuples\n******************\n",
                            numtuples,
                            node.tuple_count,
                            dataSink.count));
  }
}
