/*
 * Created by Peter Lindes, 28 August 2013
 *
 *	This test goes through all the RL related parameters
 *	and first tests that the default is set correctly.
 *
 *	Then it sets the parameter to one or more different
 *	values and reads them back to make sure it was
 *	set correctly.
 *
 */
package org.jsoar.kernel.learning.rl;


import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.ApoptosisChoices;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.ChunkStop;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.DecayMode;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.HrlDiscount;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Learning;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.LearningPolicy;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Meta;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.TemporalExtension;
import org.jsoar.kernel.learning.rl.ReinforcementLearningParams.Trace;

public class RLParamsTests extends AndroidTestCase
{
    private Agent agent;
    
    private final double DELTA = 0.0000000001;
    
    @Override
    public void setUp()
    {
        this.agent = new Agent(getContext());
    }
    
    @Override
    public void tearDown()
    {
        this.agent.dispose();
        this.agent = null;
    }
    
    public void testRLLearningParameter() throws Exception
    {
    	//	Test the default of off
    	Learning learning = agent.getProperties().get(ReinforcementLearningParams.LEARNING);
    	assertEquals(Learning.off, learning);
    	
    	//	Test setting it to on
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING, Learning.on);
    	learning = agent.getProperties().get(ReinforcementLearningParams.LEARNING);
    	assertEquals(Learning.on, learning);
    	
    	//	Test setting it back to off
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING, Learning.off);
    	learning = agent.getProperties().get(ReinforcementLearningParams.LEARNING);
    	assertEquals(Learning.off, learning);
    }
    
    public void testTemporalExtensionParameter() throws Exception
    {
    	//	Test the default of on
    	TemporalExtension extension = agent.getProperties()
    			.get(ReinforcementLearningParams.TEMPORAL_EXTENSION);
    	assertEquals(TemporalExtension.on, extension);
    	
    	//	Test setting it to off
    	agent.getProperties().set(ReinforcementLearningParams.TEMPORAL_EXTENSION, TemporalExtension.off);
    	extension = agent.getProperties().get(ReinforcementLearningParams.TEMPORAL_EXTENSION);
    	assertEquals(TemporalExtension.off, extension);
    	
    	//	Test setting it to back to on
    	agent.getProperties().set(ReinforcementLearningParams.TEMPORAL_EXTENSION, TemporalExtension.on);
    	extension = agent.getProperties().get(ReinforcementLearningParams.TEMPORAL_EXTENSION);
    	assertEquals(TemporalExtension.on, extension);
    }
    
    public void testDiscountRateParameter() throws Exception
    {
    	//	Test the default of 0.9
    	double discount_rate = agent.getProperties()
    			.get(ReinforcementLearningParams.DISCOUNT_RATE);
    	assertEquals(0.9, discount_rate, DELTA);
    	
    	//	Test setting it 0.1
    	agent.getProperties().set(ReinforcementLearningParams.DISCOUNT_RATE, 0.1);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.DISCOUNT_RATE);
    	assertEquals(0.1, discount_rate, DELTA);
    	
    	//	Test setting it to 0.0
    	agent.getProperties().set(ReinforcementLearningParams.DISCOUNT_RATE, 0.0);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.DISCOUNT_RATE);
    	assertEquals(0.0, discount_rate, DELTA);
    	
    	//	Test setting it to back 0.9
    	agent.getProperties().set(ReinforcementLearningParams.DISCOUNT_RATE, 0.9);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.DISCOUNT_RATE);
    	assertEquals(0.9, discount_rate, DELTA);
    }
    
    public void testLearningPolicyParameter() throws Exception
    {
    	//	Test the default of sarsa
    	LearningPolicy extension = agent.getProperties()
    			.get(ReinforcementLearningParams.LEARNING_POLICY);
    	assertEquals(LearningPolicy.sarsa, extension);
    	
    	//	Test setting it to off
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING_POLICY, LearningPolicy.q);
    	extension = agent.getProperties().get(ReinforcementLearningParams.LEARNING_POLICY);
    	assertEquals(LearningPolicy.q, extension);
    	
    	//	Test setting it to back to sarsa
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING_POLICY, LearningPolicy.sarsa);
    	extension = agent.getProperties().get(ReinforcementLearningParams.LEARNING_POLICY);
    	assertEquals(LearningPolicy.sarsa, extension);
    }
    
    public void testLearningRateParameter() throws Exception
    {
    	//	Test the default of 0.3
    	double learning_rate = agent.getProperties()
    			.get(ReinforcementLearningParams.LEARNING_RATE);
    	assertEquals(0.3, learning_rate, DELTA);
    	
    	//	Test setting it 0.9
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING_RATE, 0.9);
    	learning_rate = agent.getProperties().get(ReinforcementLearningParams.LEARNING_RATE);
    	assertEquals(0.9, learning_rate, DELTA);
    	
    	//	Test setting it to 0.1
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING_RATE, 0.1);
    	learning_rate = agent.getProperties().get(ReinforcementLearningParams.LEARNING_RATE);
    	assertEquals(0.1, learning_rate, DELTA);
    	
    	//	Test setting it to back 0.3
    	agent.getProperties().set(ReinforcementLearningParams.LEARNING_RATE, 0.3);
    	learning_rate = agent.getProperties().get(ReinforcementLearningParams.LEARNING_RATE);
    	assertEquals(0.3, learning_rate, DELTA);
    }
    
    public void testHrlDiscountParameter() throws Exception
    {
    	//	Test the default of off
    	HrlDiscount hrl_discount = agent.getProperties().get(ReinforcementLearningParams.HRL_DISCOUNT);
    	assertEquals(HrlDiscount.off, hrl_discount);
    	
    	//	Test setting it to on
    	agent.getProperties().set(ReinforcementLearningParams.HRL_DISCOUNT, HrlDiscount.on);
    	hrl_discount = agent.getProperties().get(ReinforcementLearningParams.HRL_DISCOUNT);
    	assertEquals(HrlDiscount.on, hrl_discount);
    	
    	//	Test setting it back to off
    	agent.getProperties().set(ReinforcementLearningParams.HRL_DISCOUNT, HrlDiscount.off);
    	hrl_discount = agent.getProperties().get(ReinforcementLearningParams.HRL_DISCOUNT);
    	assertEquals(HrlDiscount.off, hrl_discount);
    }
    
    public void testEligibilityTraceDecayRateParameter() throws Exception
    {
    	//	Test the default of 0.0
    	double discount_rate = agent.getProperties()
    			.get(ReinforcementLearningParams.ET_DECAY_RATE);
    	assertEquals(0.0, discount_rate, DELTA);
    	
    	//	Test setting it 0.9
    	agent.getProperties().set(ReinforcementLearningParams.ET_DECAY_RATE, 0.9);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.ET_DECAY_RATE);
    	assertEquals(0.9, discount_rate, DELTA);
    	
    	//	Test setting it to 0.1
    	agent.getProperties().set(ReinforcementLearningParams.ET_DECAY_RATE, 0.1);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.ET_DECAY_RATE);
    	assertEquals(0.1, discount_rate, DELTA);
    	
    	//	Test setting it to back 0.0
    	agent.getProperties().set(ReinforcementLearningParams.ET_DECAY_RATE, 0.0);
    	discount_rate = agent.getProperties().get(ReinforcementLearningParams.ET_DECAY_RATE);
    	assertEquals(0.0, discount_rate, DELTA);
    }
    
    public void testEligibilityTraceToleranceParameter() throws Exception
    {
    	//	Test the default of 0.001
    	double et_tolerance = agent.getProperties()
    			.get(ReinforcementLearningParams.ET_TOLERANCE);
    	assertEquals(0.001, et_tolerance, DELTA);
    	
    	//	Test setting it to 0.1
    	agent.getProperties().set(ReinforcementLearningParams.ET_TOLERANCE, 0.1);
    	et_tolerance = agent.getProperties().get(ReinforcementLearningParams.ET_TOLERANCE);
    	assertEquals(0.1, et_tolerance, DELTA);
    	
    	//	Test setting it to -0.1
    	agent.getProperties().set(ReinforcementLearningParams.ET_TOLERANCE, -0.1);
    	et_tolerance = agent.getProperties().get(ReinforcementLearningParams.ET_TOLERANCE);
    	assertEquals(-0.1, et_tolerance, DELTA);
    	
    	//	Test setting it to back 0.001
    	agent.getProperties().set(ReinforcementLearningParams.ET_TOLERANCE, 0.001);
    	et_tolerance = agent.getProperties().get(ReinforcementLearningParams.ET_TOLERANCE);
    	assertEquals(0.001, et_tolerance, DELTA);
    }
    
    public void testChunkStopParameter() throws Exception
    {
    	//	Test the default of on
    	ChunkStop chunk_stop = agent.getProperties()
    			.get(ReinforcementLearningParams.CHUNK_STOP);
    	assertEquals(ChunkStop.on, chunk_stop);
    	
    	//	Test setting it to off
    	agent.getProperties().set(ReinforcementLearningParams.CHUNK_STOP, ChunkStop.off);
    	chunk_stop = agent.getProperties().get(ReinforcementLearningParams.CHUNK_STOP);
    	assertEquals(ChunkStop.off, chunk_stop);
    	
    	//	Test setting it to back to on
    	agent.getProperties().set(ReinforcementLearningParams.CHUNK_STOP, ChunkStop.on);
    	chunk_stop = agent.getProperties().get(ReinforcementLearningParams.CHUNK_STOP);
    	assertEquals(ChunkStop.on, chunk_stop);
    }
    
    public void testDecayModeParameter() throws Exception
    {
    	//	Test the default of normal-decay
    	DecayMode extension = agent.getProperties()
    			.get(ReinforcementLearningParams.DECAY_MODE);
    	assertEquals(DecayMode.normal_decay, extension);
    	
    	//	Test setting it to exponential_decay
    	agent.getProperties().set(ReinforcementLearningParams.DECAY_MODE, DecayMode.exponential_decay);
    	extension = agent.getProperties().get(ReinforcementLearningParams.DECAY_MODE);
    	assertEquals(DecayMode.exponential_decay, extension);
    	
    	//	Test setting it to logarithmic_decay
    	agent.getProperties().set(ReinforcementLearningParams.DECAY_MODE, DecayMode.logarithmic_decay);
    	extension = agent.getProperties().get(ReinforcementLearningParams.DECAY_MODE);
    	assertEquals(DecayMode.logarithmic_decay, extension);
    	
    	//	Test setting it to delta_bar_delta_decay
    	agent.getProperties().set(ReinforcementLearningParams.DECAY_MODE, DecayMode.delta_bar_delta_decay);
    	extension = agent.getProperties().get(ReinforcementLearningParams.DECAY_MODE);
    	assertEquals(DecayMode.delta_bar_delta_decay, extension);
    	
    	//	Test setting it to back to normal_decay
    	agent.getProperties().set(ReinforcementLearningParams.DECAY_MODE, DecayMode.normal_decay);
    	extension = agent.getProperties().get(ReinforcementLearningParams.DECAY_MODE);
    	assertEquals(DecayMode.normal_decay, extension);
    }
    
    public void testMetaParameter() throws Exception
    {
    	//	Test the default of off
    	Meta meta = agent.getProperties().get(ReinforcementLearningParams.META);
    	assertEquals(Meta.off, meta);
    	
    	//	Test setting it to on
    	agent.getProperties().set(ReinforcementLearningParams.META, Meta.on);
    	meta = agent.getProperties().get(ReinforcementLearningParams.META);
    	assertEquals(Meta.on, meta);
    	
    	//	Test setting it back to off
    	agent.getProperties().set(ReinforcementLearningParams.META, Meta.off);
    	meta = agent.getProperties().get(ReinforcementLearningParams.META);
    	assertEquals(Meta.off, meta);
    }
    
    public void testMetaLearningRateParameter() throws Exception
    {
    	//	Test the default of 0.1
    	double meta_learning_rate = agent.getProperties()
    			.get(ReinforcementLearningParams.META_LEARNING_RATE);
    	assertEquals(0.1, meta_learning_rate, DELTA);
    	
    	//	Test setting it 0.9
    	agent.getProperties().set(ReinforcementLearningParams.META_LEARNING_RATE, 0.9);
    	meta_learning_rate = agent.getProperties().get(ReinforcementLearningParams.META_LEARNING_RATE);
    	assertEquals(0.9, meta_learning_rate, DELTA);
    	
    	//	Test setting it to 0.3
    	agent.getProperties().set(ReinforcementLearningParams.META_LEARNING_RATE, 0.3);
    	meta_learning_rate = agent.getProperties().get(ReinforcementLearningParams.META_LEARNING_RATE);
    	assertEquals(0.3, meta_learning_rate, DELTA);
    	
    	//	Test setting it to back 0.1
    	agent.getProperties().set(ReinforcementLearningParams.META_LEARNING_RATE, 0.1);
    	meta_learning_rate = agent.getProperties().get(ReinforcementLearningParams.META_LEARNING_RATE);
    	assertEquals(0.1, meta_learning_rate, DELTA);
    }
    
    public void testUpdateLogPathParameter() throws Exception
    {
    	//	Test the default of ""
    	String update_log_path = agent.getProperties()
    			.get(ReinforcementLearningParams.UPDATE_LOG_PATH);
    	assertEquals("", update_log_path);
    	
    	//	Test setting it "test.log"
    	agent.getProperties().set(ReinforcementLearningParams.UPDATE_LOG_PATH, "test.log");
    	update_log_path = agent.getProperties().get(ReinforcementLearningParams.UPDATE_LOG_PATH);
    	assertEquals("test.log", update_log_path);
    	
    	//	Test setting it to "C:\Soar\Test\test.log"
    	agent.getProperties().set(ReinforcementLearningParams.UPDATE_LOG_PATH, "C:\\Soar\\Test\\test.log");
    	update_log_path = agent.getProperties().get(ReinforcementLearningParams.UPDATE_LOG_PATH);
    	assertEquals("C:\\Soar\\Test\\test.log", update_log_path);
    	
    	//	Test setting it to back ""
    	agent.getProperties().set(ReinforcementLearningParams.UPDATE_LOG_PATH, "");
    	update_log_path = agent.getProperties().get(ReinforcementLearningParams.UPDATE_LOG_PATH);
    	assertEquals("", update_log_path);
    }
    
    public void testApoptosisParameter() throws Exception
    {
    	//	Test the default of none
    	ApoptosisChoices apoptosis = agent.getProperties()
    			.get(ReinforcementLearningParams.APOPTOSIS);
    	assertEquals(ApoptosisChoices.none, apoptosis);
    	
    	//	Test setting it to chunks
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS, ApoptosisChoices.chunks);
    	apoptosis = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS);
    	assertEquals(ApoptosisChoices.chunks, apoptosis);
    	
    	//	Test setting it to rl-chunks
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS, ApoptosisChoices.rl_chunks);
    	apoptosis = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS);
    	assertEquals(ApoptosisChoices.rl_chunks, apoptosis);
    	
    	//	Test setting it to back to normal_decay
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS, ApoptosisChoices.none);
    	apoptosis = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS);
    	assertEquals(ApoptosisChoices.none, apoptosis);
    }
    
    public void testApoptosisDecayParameter() throws Exception
    {
    	//	Test the default of 0.5
    	double apoptosis_decay = agent.getProperties()
    			.get(ReinforcementLearningParams.APOPTOSIS_DECAY);
    	assertEquals(0.5, apoptosis_decay, DELTA);
    	
    	//	Test setting it to 0.1
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_DECAY, 0.1);
    	apoptosis_decay = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_DECAY);
    	assertEquals(0.1, apoptosis_decay, DELTA);
    	
    	//	Test setting it to 0.0
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_DECAY, 0.0);
    	apoptosis_decay = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_DECAY);
    	assertEquals(0.0, apoptosis_decay, DELTA);
    	
    	//	Test setting it to 0.9
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_DECAY, 0.9);
    	apoptosis_decay = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_DECAY);
    	assertEquals(0.9, apoptosis_decay, DELTA);
    	
    	//	Test setting it to back 0.5
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_DECAY, 0.5);
    	apoptosis_decay = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_DECAY);
    	assertEquals(0.5, apoptosis_decay, DELTA);
    }
    
    public void testApoptosisThreshParameter() throws Exception
    {
    	//	Test the default of -2.0
    	double apoptosis_thresh = agent.getProperties()
    			.get(ReinforcementLearningParams.APOPTOSIS_THRESH);
    	assertEquals(-2.0, apoptosis_thresh, DELTA);
    	
    	//	Test setting it to 2.0
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_THRESH, 2.0);
    	apoptosis_thresh = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_THRESH);
    	assertEquals(2.0, apoptosis_thresh, DELTA);
    	
    	//	Test setting it to 0.0
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_THRESH, 0.0);
    	apoptosis_thresh = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_THRESH);
    	assertEquals(0.0, apoptosis_thresh, DELTA);
    	
    	//	Test setting it to 0.9
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_THRESH, 0.9);
    	apoptosis_thresh = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_THRESH);
    	assertEquals(0.9, apoptosis_thresh, DELTA);
    	
    	//	Test setting it to back -2.0
    	agent.getProperties().set(ReinforcementLearningParams.APOPTOSIS_THRESH, -2.0);
    	apoptosis_thresh = agent.getProperties().get(ReinforcementLearningParams.APOPTOSIS_THRESH);
    	assertEquals(-2.0, apoptosis_thresh, DELTA);
    }
    
    public void testTraceParameter() throws Exception
    {
    	//	Test the default of off
    	Trace trace = agent.getProperties().get(ReinforcementLearningParams.TRACE);
    	assertEquals(Trace.off, trace);
    	
    	//	Test setting it to on
    	agent.getProperties().set(ReinforcementLearningParams.TRACE, Trace.on);
    	trace = agent.getProperties().get(ReinforcementLearningParams.TRACE);
    	assertEquals(Trace.on, trace);
    	
    	//	Test setting it back to off
    	agent.getProperties().set(ReinforcementLearningParams.TRACE, Trace.off);
    	trace = agent.getProperties().get(ReinforcementLearningParams.TRACE);
    	assertEquals(Trace.off, trace);
    }
    
}
