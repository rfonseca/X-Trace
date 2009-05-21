
module XTrace
  class Util
    def Util.stringToBytes(s)
      unless ((s.length % 2) !=0)
        bytes = Array.new(s.length/2,0x00)
        bytes.each_index do |i|
          bytes[i] = bytes[i] | s.slice(i*2,2).to_i(16) # each two digits are one byte of hex
        end
        return bytes
      else
        puts "error"
        return nil
      end
    end
  
    def Util.bytesToString(bytes,offset,length)
      # TODO: use offset and length
      eachByte = Array.new()
      bytes.each do |x|
          b = x.to_s(16).upcase
          if b.length <2
            b = "0"+b
          end
          eachByte << b
        end
      return eachByte.join("")
    end
  
    def Util.generateHex(numChars)
        validChars = ("A".."F").to_a + ("0".."9").to_a
        length = validChars.size

        hexCode = ""
        1.upto(numChars) { |i| hexCode << validChars[rand(length-1)] }

        hexCode
    end
    
    def Util.copyArray(source,offset,length)
      dest = Array.new
      offset.upto(length+offset-1) do |i|
        dest << source[i]
      end
      return dest
    end
  end

  class OptionField
    attr_reader :type, :payload
    
    def initialize(type=nil,plbytes=nil,ploffset=nil,pllength=nil)
      # TODO: do something with offset and length
      unless type.nil?
        @type = type 
        if !plbytes.nil? and plbytes.class==Array # have an array of payload bytes
          if !ploffset.nil? and !pllength.nil? and pllength >0  and !(pllength >256)# make payload start at offset (and with given length?)
            @payload.Array.new()
            ploffset.upto(pllength+ploffset-1) do |i| # out of bounds error possible?
              @payload << plbytes[i]
            end
          else
            @payload = Array.new(plbytes)
          end
        else
          @payload = nil
        end
      else
        @type = 0x00 # default to "no option" type
        @payload = nil
      end 
    end
    
    def OptionField.createFromBytes(bytes,offset,length)
      if bytes.nil?
        puts "error"
        return OptionField.new
      end
      if bytes.length-offset < length
        puts "error"
        return OptionField.new
      end
      if length > 258
        puts "error"
        return OptionField.new
      end
      
      return OptionField.new(bytes[offset], bytes, offset+2, length - offset - 2)
    end
    
    def OptionField.createFromString(s)
      if s.empty?
        puts "error"
        return OptionField.new
      end
      
      bytes = Util.stringToBytes(s)
      return createFromBytes(bytes, 0, bytes.length)
    end  
    
    def pack
      unless @payload.nil?
        return [@type,@payload.length].concat(@payload)
      else
        return [@type,0x02]
      end
    end
    
    def to_s
      return Util.bytesToString(self.pack)
    end
  end

  class TaskID
    attr_reader :tid
    
    def initialize(length=nil,prefix=nil)
      unless length.nil?
        @tid = Array.new(length,0x00)
        unless prefix.nil? # start id with these bytes, then fill with random
          @tid.each_index do |i|
            if prefix[i].nil? # done using prefix, get random
              @tid[i] |= Util.generateHex(2).to_i(16)
            else
              @tid[i] |= prefix[i]
            end
          end
        else # fill whole thing with random
          @tid.each_index do |i|
            @tid[i] |= Util.generateHex(2).to_i(16)
          end
        end
      else # no length given, use default and create invalid task id
        @tid = Array.new(4,0x00)
      end
    end
    
    def initialize_copy(orig)
      @tid = Array.new(orig.tid)
    end
    
    def TaskID.createFromString(s)
      unless s.nil?
        bytes = Util.stringToBytes(s)
        t = self.createFromBytes(bytes, 0, bytes.length)
        unless t.nil?
          return t
        else
          puts "error"
          return TaskID.new # return invalid
        end
      else
        puts "error"
        return TaskID.new # return invalid
      end
    end
    
    def TaskID.createFromBytes(bytes,offset, length)
      # TODO: error checking
      
      taskid = Array.new
      offset.upto(length+offset-1) do |i|
        taskid << bytes[i]
      end
      
      if (length==4 or length==8 or length==12 or length==20)
        tid = TaskID.new(length,taskid)
        #puts "in taskid: length for #{tid} is #{length}"
        return tid
      end
    end
  
    def pack
      return @tid
    end
    
    def length
      return @tid.length
    end
  
    def to_s
      XTrace::Util.bytesToString(@tid,0,@tid.length)
    end
  end
end

require 'XTraceContext'
require 'XTraceEvent'
require 'reporting/Report'
require 'reporting/Reporter'