module RSoar
  
  OutputCommand = Struct.new("OutputCommand", :handler, :options)
  
  class Output
    include org.jsoar.util.events.SoarEventListener

    def initialize(agent)
      @agent = agent
      @status_sym = @agent.symbols.create_string 'status'
      @commands = {}
      
      @agent.event_manager.add_listener org.jsoar.kernel.events.OutputEvent.java_class, self
      
    end
    
    def when(name, options = {}, &block)
      @commands[name.to_s] = OutputCommand.new(block, options)
    end
    
    def on_event(event)
      io = @agent.input_output
      io.get_pending_commands.each do |wme|
        value = wme.value
        if (command = @commands[wme.attribute.value])
          r = command.handler.call value
          if r && !value.asIdentifier.nil?
            io.add_input_wme value, @status_sym, @agent.symbols.create_string(r.to_s)
          end
        end      
      end
    end
  end  
end