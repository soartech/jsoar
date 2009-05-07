
module RSoar
  class RAgent < org.jsoar.kernel.Agent
    
    attr_reader :input
    attr_reader :output
    attr_reader :tcl
    
    def initialize(&block)
      super
  
      @threaded = org.jsoar.runtime.ThreadedAgent.attach self
      @tcl = org.jsoar.tcl.SoarTclInterface.find_or_create self
  
      init_method = @threaded.java_class.declared_method(:initialize)
      init_method.invoke @threaded.java_object
  
      org.jsoar.kernel.io.CycleCountInput.new input_output, event_manager
      @input = RSoar::Input.new self
      @output = RSoar::Output.new self

      yield self if block_given?
    end
  
    def sp(body)
      get_productions.load_production body
    end
    
    def source(file)
      eval "source {#{file}}"
    end
    
    def print(s)
      printer.print s
      printer.flush
    end
    
    def eval(script) tcl.eval script  end
  
    def is_running
      @threaded.is_running
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
    
    def input_link
      input.root
    end
    
    def output_link
      output      
    end
  end
end