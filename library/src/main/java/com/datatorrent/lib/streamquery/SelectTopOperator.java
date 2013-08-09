package com.datatorrent.lib.streamquery;

import java.util.ArrayList;
import java.util.Map;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.Operator;

/**
 * This operator provides sql top select query semantic on live data stream. <br>
 * Stream rows passing condition are emitted on output port stream. <br>
 * <br>
 * <b>StateFull : NO,</b> all row data is processed in current time window. <br>
 * <b>Partitions : Yes, </b> No Input dependency among input rows. <br>
 * <br>
 * <b>Ports</b>:<br>
 * <b> inport : </b> Input hash map(row) port, expects
 * HashMap&lt;String,Object&gt;<<br>
 * <b> outport : </b> Output hash map(row) port, emits
 * HashMap&lt;String,Object&gt;<br>
 * <br>
 * <b> Properties : <b> <br>
 * <b> topValue : </b> top values count. <br>
 * <b> isPercentage : </b> top values count is percentage flag.
 * <br>
 */
public class SelectTopOperator implements Operator
{
  private ArrayList<Map<String, Object>> list;
  private int topValue = 1;
  private boolean isPercentage = false;
  
  /**
   * Input port.
   */
  public final transient DefaultInputPort<Map<String, Object>> inport = new DefaultInputPort<Map<String, Object>>() {
    @Override
    public void process(Map<String, Object> tuple)
    {
      list.add(tuple);
    }
  };
  
  @Override
  public void setup(OperatorContext context)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void teardown()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void beginWindow(long windowId)
  {
    list = new ArrayList<Map<String, Object>>();
  }

  @Override
  public void endWindow()
  {
      int numEmits = topValue;
      if (isPercentage) {
        numEmits = list.size() * (topValue/100);
      }
      for (int i=0; (i < numEmits)&&(i < list.size()); i++) {
        outport.emit(list.get(i));
      }
  }

  public int getTopValue()
  {
    return topValue;
  }

  public void setTopValue(int topValue) throws Exception
  {
    if (topValue <= 0) {
      throw new Exception("Top value must be positive number.");
    }
    this.topValue = topValue;
  }

  public boolean isPercentage()
  {
    return isPercentage;
  }

  public void setPercentage(boolean isPercentage)
  {
    this.isPercentage = isPercentage;
  }

  /**
   * Output port.
   */
  public final transient DefaultOutputPort<Map<String, Object>> outport =  new DefaultOutputPort<Map<String, Object>>();
}
