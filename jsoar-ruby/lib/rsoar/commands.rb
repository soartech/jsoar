module RSoar
  class RubyCommand
    import org.jsoar.util.commands.SoarCommand

    def initialize(binding)
      @binding = binding
    end

    # Implement SoarCommand.execute(String[] args) throws SoarCommand
    def execute(args)
      begin
        if args.length > 1
          eval args.to_a[1..-1].join(' '), @binding
        else
          "ERROR:#{args[0]}: No argument given"
        end
      rescue Exception => e
        "ERROR:#{args[0]}: #{e}"
      end
    end
  end
end
