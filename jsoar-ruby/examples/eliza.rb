require 'rsoar'

import org.jsoar.kernel.SoarProperties

agent = RSoar::RAgent.new do |a|
  a.name = "Eliza"

  a.debugger_provider = org.jsoar.debugger.JSoarDebugger.new_debugger_provider

  a.trace.disable_all
  
  a.eval 'watch 0'
  a.eval 'waitsnc --on'
  
  # a.properties[SoarProperties::WAITSNC] = true
  
  # Do something when there's a ^test output command
  a.output_link.when :test do |v|
    puts "test command with value #{v}"
    'complete' # add ^status complete
  end
  
  # Do something when there's an ^another output command
  a.output_link.when :another do |v|
    puts "another command with value #{v}"
    'error' # add ^status error
  end
  
  # Load some productions, or just do "a.source file_or_url"
  a.sp 'wait*immediately
    (state <s> ^superstate nil)
  -->
    (write (crlf) |Waiting for messages| (crlf))
    (wait)
  '
  
  a.sp 'monitor*message
    (state <s> ^superstate nil 
               ^io.input-link.message <message>)
  -->
    (write <message> | ... O RLY?| (crlf))
    (wait)
  '
    
  a.sp 'output
    (state <s> ^superstate nil ^io.output-link <ol>)
  -->
    (<ol> ^test.foo 99)
  '
  a.sp 'output*another
    (state <s> ^superstate nil ^io.output-link <ol>)
  -->
    (<ol> ^another.blah 99)
  '
  
  # Start the agent running in its own thread
  a.run_forever
end

# Read input from the user and stick it on the input link
puts "Enter a message (ctrl-Z (EOF) to exit):"
while (message = gets)
  message.chomp!
  if message.length > 0
    agent.input_link.message = message.chomp    
  end
end

puts "exiting"

exit