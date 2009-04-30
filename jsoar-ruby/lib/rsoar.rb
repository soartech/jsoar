# Add lib to load path
$LOAD_PATH.unshift File.dirname(__FILE__)

require 'java'

Dir.glob(File.join($JSOAR_HOME, 'lib', '*.jar')).each do |f|
  require f
end

require 'rsoar/input'
require 'rsoar/agent'
require 'rsoar/properties'
