
module RSoar
  class RAgent
    
    attr_reader :agent
    attr_reader :input
    attr_reader :output
    
    def initialize(&block)
      super
  
      @agent = org.jsoar.runtime.ThreadedAgent.create
  
      org.jsoar.kernel.io.CycleCountInput.new @agent.input_output
      org.jsoar.kernel.io.TimeInput.new @agent.input_output
      
      @input = RSoar::Input.new @agent.input_output
      @output = RSoar::Output.new self

      yield self if block_given?
    end

    def dispose
      agent.dispose
    end

    def sp(body)
      agent.get_productions.load_production body
    end
    
    def source(file)
      eval "source {#{file}}"
    end
    
    def print(s)
      agent.printer.print s
      agent.printer.flush
    end
    
    def eval(script) agent.interpreter.eval script  end
  
    def input_link
      input.root
    end
    
    def output_link
      output      
    end
    
    def method_missing(method_name, *args)
      agent.send method_name, *args          
    end
  end
end
