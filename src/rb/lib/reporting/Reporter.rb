module XTrace
  class Reporter
    # reporter object for class
    @reporter = nil
    # ip of receiving machine
    Udp_ip = "127.0.0.1"
    # port on receiving machine
    Udp_port = 7831
    # ip of local machine
    @local_ip
    # port on local machine
    @local_port
    
    def initialize
      # set up udp socket
      @sock = UDPSocket.new
      #@sock.bind(Udp_ip,Udp_port)
      
      # set up sender udp host and port, same as receiving machine for now (local)
      @local_ip = Udp_ip
      @local_port = 0
    end
    
    def Reporter.getReporter
      if @reporter.nil?
        return Reporter.new
      else 
        return @reporter
      end
    end
    
    def sendReport(r)
      #@sock.connect(Udp_ip,Udp_port)
      #puts "in reporter.sendreport: going to send #{r.to_s}"
      @sock.send(r.to_s, 0,Udp_ip,Udp_port)
      self.close
    end
    
    def close
      unless @reporter.nil?
        @sock.flush
        @sock.close
        @reporter = nil
      end
    end
    
    def closed?
      unless @reporter.nil?
        return @reporter.closed?
      else
        return true
      end
    end
    
  end
end