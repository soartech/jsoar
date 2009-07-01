module RSoar
  
  OutputCommand = Struct.new("OutputCommand", :handler, :options)
  
  class OutputId
    def initialize(agent, id)
      @agent = agent
      @id = id
    end
    
    def [](name)
      w = org.jsoar.kernel.memory.Wmes.matcher(@agent).attr(name.to_s).find(@id)
      if !w.nil?
        v = w.value
        v.asIdentifier.nil? ? v.value : OutputId.new(@agent, v)
      else
        nil
      end      
    end

    def root
      @id
    end
    
    def method_missing(method_name, *args)
      self[method_name]
    end
  end
  
  class Output
    include org.jsoar.util.events.SoarEventListener

    def initialize(agent)
      @agent = agent
      @status_sym = @agent.symbols.create_string 'status'
      @commands = {}
      
      @agent.events.add_listener org.jsoar.kernel.events.OutputEvent.java_class, self
    end
    
    def when(name, options = {}, &block)
      @commands[name.to_s] = OutputCommand.new(block, options)
    end
    
    def on_event(event)
      io = @agent.input_output
      io.get_pending_commands.each do |wme|
        value = wme.value
        if (command = @commands[wme.attribute.value])
          handle_command io, command, value
        end      
      end
    end
    
    def handle_command(io, command, value)
      r = nil
      begin
        r = command.handler.call OutputId.new(@agent.agent.agent, value)
      rescue Exception => e
        @agent.print "ERROR: #{e}"
        r = "error"   
      end
      if r && !value.asIdentifier.nil?
        io.add_input_wme value, @status_sym, @agent.symbols.create_string(r.to_s)
      end
    end
  end  
end
