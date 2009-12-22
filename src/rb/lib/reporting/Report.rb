module XTrace
  class Report
    
    def initialize
      @map = Hash.new # key --> array of values
    end
    
    def put(key, value, append=true)
      if @map.include?(key.to_sym) and append # have this key, and want to append a value
        valueArray = @map[key.to_sym]
      else # this is a new key
        valueArray = Array.new
      end
      
      valueArray << value
      @map[key.to_sym] = valueArray
      #puts "in report: put #{key}, #{value}"
    end
    
    def get(key)
      @map[key.to_sym]
    end
    
    def remove(key)
      @map.delete(key.to_sym)
    end
    
    def getMetadata 
      mdArray = @map[:"X-Trace"]
      #puts "in report: md from report is #{mdArray.first.to_s}"
      unless mdArray.nil? or mdArray.empty?
        md = XTraceMetadata.createFromString(mdArray.first) # should only be one
        #puts "in report: md is #{md.to_s}"
        return md
      else
        return nil
      end
    end
    
    def to_s
      temp = Array.new
      @map.each_pair do |key,valArr|
        valArr.each do |val|
          temp << key.to_s + ": " + val.to_s
        end
      end
      temp.insert(0,"X-Trace Report ver 1.0")
      return temp.join("\n")
    end
    
    def createFromString(s)
      # TODO
    end
    
    def eql?(object)
      self == (object)
    end
    
    def == (object)
      object.equal?(self) or (object.instance_of?(self.class) and object.to_s == self.to_s)
    end
    
  end
end
