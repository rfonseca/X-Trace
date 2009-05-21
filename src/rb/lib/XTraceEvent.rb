require 'time'
require 'XTraceMetadata'
require 'reporting/Report'

module XTrace
  
  class XTraceEvent
    
    def initialize(opidlength)
      @report = Report.new
      #@opId = Array.new(opidlength,0x00)
      byteStr = Util.generateHex(opidlength*2)
      @opid = Util.stringToBytes(byteStr)
    end
    
    def addEdge(md)
      unless md.nil? or !md.valid?
        newmd = md.dup
        #puts "in event: md is #{md.to_s}"
        newmd.setOpId(@opid)
        #puts "in event: newmd is #{newmd.to_s}"
        
        @report.put("X-Trace",newmd.to_s,false)
        @report.put("Edge",md.getOpIdString)
      end
      # else return nothing
    end
    
    def put(key,val)
      @report.put(key,val)
    end
    
    def getNewMetadata
      md = @report.getMetadata
      unless md.nil?
        #puts "in event: report has ctx #{md.to_s}"
        #puts "in event: #{md.taskid}"
        md2 = md.dup
        md2.setOpId(@opid)
        return md2
      else
        return XTraceMetadata.new # return invalid one
      end
    end
    
    def setMetadata(md)
      @opid = Array.new(md.getOpId)
      @report.put("X-Trace",md.to_s,false)
    end
    
    def setTimestamp
      ts = "%10.5f" % Time.now.to_f # seconds since epoch with five decimal places
      @report.put("Timestamp", ts,false)
    end
    
    def createReport
      self.setTimestamp
      return @report
    end
    
    def sendReport
      self.setTimestamp
      Reporter.getReporter.sendReport(@report)
    end
  end
end