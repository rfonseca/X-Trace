require 'socket'
require 'XTraceMetadata'
require 'XTraceEvent'

module XTrace
  
  class XTraceContext
    # The context's metadata
    @context = XTraceMetadata.new
    # Hostname of current  machine
    @host = nil;
    # Default length of opid
    DefaultOpidLength = 8
    
    def XTraceContext.startTrace(agent,title,*tags)
      taskid = TaskID.new(8) # 8-byte taskid
      opid = Util.stringToBytes(Util.generateHex(16)) # 8-byte opid
      self.setContext(XTraceMetadata.new(taskid,opid))
      #puts "in context: start trace ctx is #{self.getContext.to_s}"
      event = self.createEvent(agent, "Start Trace: " + title)
      event.put("Title", title)
      unless tags.empty?
        tags.each do |t|
          event.put("Tag", t.to_s)
        end
      end
  		event.sendReport
    end
    
    def XTraceContext.setContext(ctx)
      if !ctx.nil? and ctx.valid?
        @context = ctx
      else
        @context = nil
      end
    end
    
    def XTraceContext.getContext
      @context
    end
    
    def XTraceContext.clearContext
      @context = nil
    end
    
    def XTraceContext.logEvent(agent,label, args={})
      unless !self.valid?
        event = self.createEvent(agent,label)
        args.each_pair do |key, val|
          event.put(key.to_s, val.to_s)
        end
        event.sendReport
      end
    end
    
    def XTraceContext.createEvent(agent, label)
      if !self.valid?
        return nil
      end
      
      oldctx = self.getContext
      opidlength = DefaultOpidLength # set to default
      unless oldctx.nil?
        opidlength = oldctx.getOpIdLength # override default if have it
      end
      event = XTraceEvent.new(opidlength)
      event.addEdge(oldctx) # could be nil?
      
      unless !@host.nil? # try to get hostname if don't have it already
        @host = Socket.gethostname # catch exception?
      end
      
      # load the info into the event
      event.put("Host",@host)
      event.put("Agent",agent)
      event.put("Label", label)
      
      # clean up
      #puts "in context: old ctx is #{oldctx.to_s}"
      self.setContext(event.getNewMetadata)
      #puts "in context: new ctx is #{self.getContext.to_s}"
      return event
    end
    
    def XTraceContext.valid?
      !self.getContext.nil?
    end
    
  end
end