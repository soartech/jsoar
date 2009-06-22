# Add lib to load path
$LOAD_PATH.unshift File.dirname(__FILE__)

require 'java'

# If we're in a normal JSoar distribution, then JSOAR_HOME should be
# two directories up
if !ENV['JSOAR_HOME']
  ENV['JSOAR_HOME'] = File.join(File.dirname(__FILE__), '..', '..')
end

if ENV['JSOAR_HOME']
  Dir.glob(File.join(ENV['JSOAR_HOME'], 'lib', '*.jar')).each do |f|
    require f
  end
end

require 'rsoar/input'
require 'rsoar/output'
require 'rsoar/agent'
require 'rsoar/properties'
