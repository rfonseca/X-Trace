require 'xtrace'

module XTrace
  
  class XTraceMetadata
    
    InvalidId = [0x00,0x00,0x00,0x00]
    MetadataVersion = 0x01
    
    attr_reader :taskid
    
    # constructor creates blank invalid object if id and opid are not provided
    def initialize(id=nil, opIdbytes=nil)
      #puts "in metadata.init: args are taskid #{id} and opid #{opIdbytes}"
      if !id.nil? and !opIdbytes.nil? and opIdbytes.class==Array
        @taskid = id.dup
        @opId = Array.new(opIdbytes)
      else
        @taskid = TaskID.createFromBytes(InvalidId,0,InvalidId.length)
        @opId = [0x00,0x00,0x00,0x00]
      end
      @options = nil
      @numOptions = 0
      #puts "in metadata: tid is #{@taskid.to_s}, opid is #{@opId}"
    end
    
    def initialize_copy(orig)
      @taskid = orig.taskid.dup
      @opId = Array.new(orig.getOpId)
      @optoins = nil # TODO - copy options and num options too
      @numOptions = 0
    end
    
    def XTraceMetadata.createFromBytes(bytes,offset,length)
      # TODO: use offset?
      
       #puts "in metadata: bytes is #{Util.bytesToString(bytes,0,bytes.length)}"
      if bytes.nil?
        puts "metadata: error"
        return XTraceMetadata.new
      end
      if offset < 0
        puts "metadata: error"
        return XTraceMetadata.new
      end 
      if length < 9
        puts "metadata: error"
        return XTraceMetadata.new
      end 
      
      # get task id length
      taskidlength = 0
      case bytes[0] & 0b00000011
        when 0x00
          taskidlength = 4
        when 0x01
          taskidlength = 8
        when 0x02
          taskidlength = 12
        when 0x03
          taskidlength = 20
        else # can't happen
      end
      
      # get opid length, checking if fifth bit is 1 or 0
      if ( (bytes[0] & 0x08) !=0)
        opidlength = 8 
      else
        opidlength = 4
      end
      
      # make sure flags don't imply a length that is too long
      if taskidlength+opidlength >  length
        return XTraceMetadata.new
      end
      
      # create the taskid and opid fields
      taskid = TaskID.createFromBytes(bytes,1,taskidlength)
      #puts "in metadata: created taskid #{taskid.to_s}"
      opIdbytes = Array.new
      opidoffset = 1+taskidlength # offset length of flags and taskid
      opidoffset.upto(opidlength+opidoffset-1) do |i|
        opIdbytes << bytes[i]
      end
      #puts "in metadata: created opid #{opIdbytes}"
      
      md = XTraceMetadata.new(taskid,opIdbytes)
      
      # if there are no options, we're done
      if ((bytes[0] & 0x04) == 0)
        return md
      end
      
      # else read in option length
      if length <= (1 + taskidlength + opidlength)
        puts "error"
        return XTraceMetadata.new 
      end
      
      totalOptLength = bytes[1+taskidlength+opidlength] # get options length byte
      optPos = offset+1+taskidlength+opidlength+1 # start at postion past the flags,taskid,opid,and the opt length
      
      until totalOptLength <2 do
        opttype = bytes[optPos]
        optPos +=1
        optlength = bytes[optPos]
        optPos +=1
        if (optlength > totalOptLength)
          puts "error: Invalid option length"
          break
        end
        
        if optlength >0
          o = OptionField.createFromBytes(bytes,optPos,optlength)
        else
          o = OptionField.new(opttype,nil)
        end
        md.addOption(o)
        
        # increment/decrement for opt type+length+payload
        totalOptLength -= (2+optlength)
        optPos += (2+ optlength)
      end
      
      return md
    end
    
    def XTraceMetadata.createFromString(s)
      unless s.nil?
        bytes = Util.stringToBytes(s)
        md = self.createFromBytes(bytes, 0, bytes.length)
        unless md.nil?
          return md
        else
          puts "error"
          return XTraceMetadata.new # return invalid
        end
      else
        puts "error"
        return XTraceMetadata.new # return invalid
      end
    end
    
    def pack
      packed = Array.new # byte array
      
      # flags
      flags = 0x00
      case @taskid.length
      when 4
        flags = 0x00
      when 8
        flags = 0x01
      when 12
        flags = 0x02
      when 20
        flags = 0x03
      else # shouldn't happen
      end
      
      flags |= (MetadataVersion << 4)
      if !self.getOptions.nil? and getOptions.length >0
        flags |= 0x04
      end
      if @opId.length==8
        flags |= 0x08
      end
      packed << flags
      
      # taskid
      @taskid.pack.each do |b|
        packed << b
      end
      
      # opid
      @opId.each do |b|
        packed << b
      end
      
      # options  
      if !self.getOptions.nil?  
        opts = Array.new(self.getOptions)
        optLenPosition = packed.length # position in byte array where length will go (after computed it)
        totalOptLength = 0x00
        
        # placeholder for the total options length byte
        if (!opts.nil? and opts.length >0)
          packed << 0x00
        end
        
        # put all the options in
        opts.each do |o|
          unless o.nil? # don't use the null ones we padded @options with to amortize insert cost
            optBytes = o.pack
            totalOptLength += optBytes.length
            optBytes.each do |b| 
              packed << b
            end
          end
        end
        
        # calculate how big all the options where and set that byte
        packed[optLenPosition] = totalOptLength
      end
      
      return packed
    end
  
    def valid?
      @taskid.pack.each do |byte|
        if (byte | 0x00)!=0x00 # just checking if some byte is nonzero
          return true 
        end
      end
      return false
    end
    
    def getOptions
      unless @numOptions < 1
        return @options
      else
        return nil
      end
    end
    
    def addOption(opt)
      if @numOptions==0
        @options = Array.new
      elsif @numOptions==@options.length # filled the current length of array
        tmp = @options
        @options = Array.new(tmp.length*2,nil) # make new array twice as big
        tmp.each_index do |i|# copy the previous content into the new array
          @options[i] = tmp[i]
        end
      end
      @numOptions +=1
      @options[@numOptions]=opt
    end
    
    def getVersion
      MetadataVersion.to_i 
    end
    
    def setOpId(opid)
      # TODO: error checking
      #puts "in metadata: in opid is #{Util.bytesToString(opid,0,opid.length)}"
      @opId = Array.new(opid) # copy the array
    end
    
    def getOpId
      @opId
    end
    
    def getOpIdString
      return Util.bytesToString(@opId,0,@opId.length)
    end
    
    #def getTaskId
    #  @taskid
    #end
    
    def to_s
      bytes = self.pack
      unless bytes.nil? or bytes.empty?
        return Util.bytesToString(bytes,0,bytes.length)
      else
        return "error"
      end
    end
    
    def getOpIdLength
      @opId.length
    end
    
  end
end