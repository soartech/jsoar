module RSoar
  class InputId
   
    attr_reader :char
    attr_reader :map
  
    def initialize(tree, char = 'Z')
      @tree = tree
      @char = char
      @map = Hash.new { |hash, key| hash[key] = [] }
    end
  
    def add_attribute(name, value = nil, &block)
      value = new_id(name.to_s) if value.nil?
      @tree.enqueue Proc.new { |t| t.do_add self, name, value }
      yield value if block_given?
      value
    end
  
    def remove_attribute(name)
      @tree.enqueue Proc.new { |t| t.do_remove self, name }
      name
    end
  
    def update_attribute(name, value = nil, &block)
      value = new_id(name.to_s) if value.nil?
      @tree.enqueue Proc.new { |t| t.do_update self, name, value }
      yield value if block_given?
      value
    end
  
    # Construct a new id , pass it to the block and return it
    def new_id(char = 'Z', &block)
      id = InputId.new @tree, char
      yield id if block_given?
      id
    end
  
    def method_missing(method_name, *args)
      if /=$/=~ (method_name=method_name.to_s) then
        add_attribute method_name[0...-1], *args
      else
        super
      end
    end
  
    # Add an attribute/value to this id
    def + (name, value = nil, &block) add_attribute name, value, &block end
    
    # Remove all attributes with the given name from this id
    def - (name)  remove_attribute name end
      
    # Update the WME with the given attribute name with a new value
    def ^ (name, value = nil, &block) update_attribute name, value, &block end
  end
  
  class Input
    include org.jsoar.util.events.SoarEventListener
  
    attr_reader :root
  
    def initialize(io)
      @io = io
  
      @queue = java.util.LinkedList.new
      @io.events.add_listener org.jsoar.kernel.events.InputEvent.java_class, self
  
      @root = InputId.new self
      @ids = {}
    end
  
    # Execute a set of WM modifications atomically
    def atomic(&block)
      @queue.synchronized do
        yield self
      end
    end
    
    def enqueue(op)
      @queue.synchronized do
        @queue.addLast op
      end
      @io.asynchronous_input_ready
    end
  
    def do_add(node, name, value)
      if value.is_a? InputId
        value = create_id_symbol value
      end
      
      id = create_id_symbol node
      wme = org.jsoar.kernel.io.InputWmes.add @io, id, @io.symbols.create_string(name.to_s), value
      node.map[name] << wme
    end
  
    def do_remove(node, name)
      node.map[name].each do |wme|
        wme.remove
      end
      node.map.delete name
    end
  
    def do_update(node, name, value)
      do_remove node, name
      do_add node, name, value 
    end
    
    private
    
    def get_id_symbol(node)
      @root == node ? @io.input_link : @ids[node]
    end
  
    def create_id_symbol(node)
      id = get_id_symbol node
  
      if id.nil?
        id = @io.symbols.create_identifier node.char[0]
        @ids[node] = id
      end
  
      id 
    end
  
    def on_event(event)
      @queue.synchronized do
        while !@queue.is_empty
          @queue.removeFirst.call self
        end
      end
    end

  end
end
