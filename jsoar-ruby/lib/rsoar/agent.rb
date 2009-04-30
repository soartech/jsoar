
module RSoar
  class RAgent < org.jsoar.kernel.Agent
    
    def initialize(&block)
      super
  
      @threaded = org.jsoar.runtime.ThreadedAgent.attach self
  
      init_method = @threaded.java_class.declared_method(:initialize)
      init_method.invoke @threaded.java_object
  
      org.jsoar.kernel.io.CycleCountInput.new input_output, event_manager
      if block_given?
        yield self
      end
  
    end
  
    def sp(body)
      get_productions.load_production body
    end
  
    def run_for(n, type)
      @threaded.run_for n, type
    end
    def run_forever
      @threaded.run_for 0, org.jsoar.kernel.RunType::FOREVER
    end
  
    def debug
      debugger_provider.open_debugger self
    end
  end
end