class HadoopReport
  #a list of reports
  attr_reader :json_tasks, :json_maps, :json_reduces, :suspects, :report_times,
              :maps, :reduces, :bad_tasks, :map_stats, :reduce_stats

  # accept an array of json formatted tasks
  def initialize (task_array)
    @maps, @reduces, @bad_tasks, @suspects, @report_times = [], [], [], [], []
    @map_stats, @reduce_stats = StatCounter.new, StatCounter.new
    @json_maps = task_array["maps"]
    @json_reduces = task_array["reduces"]
    @json_tasks = @json_maps + @json_reduces
    set_tasks
    set_suspects
    set_report_times
    set_stats
  end

  def length;   @json_tasks.length       end
  def empty?;   tasks.empty?             end
  def start;    report_times.min/1000.0  end
  def finish;   report_times.max/1000.0  end
  def duration; finish - start           end
 
  def tasks
    if not @maps.nil? and not @reduces.nil?
      return @maps + @reduces
    else
      return []
    end
  end
 
  def task_stats
    if not @map_stats.nil? and not @reduce_stats.nil?
      return @map_stats + @reduce_stats
    else
      return []
    end
  end

  def to_s
    out = "HadoopReport details:"
    if not @maps.empty? : out += " #{@maps.length} maps" end
    if not @reduces.empty? : out += ", #{@reduces.length} reduces" end
    return out
  end

  def set_report_times
    @json_tasks.each do |tip|
      tip["tasks"].each do |task|
        start = task["startTime"]
        finish = task["finishTime"]
        @report_times << start if start != 0
        @report_times << finish if finish != 0
      end
    end
  end

  def set_tasks
   [@json_maps, @json_reduces].each_with_index do |task_type,i| 
     is_reduce = (i == 1)
     task_type.each do |tip|
       tip["tasks"].each do |task| 
         
         #create new task [tid, start_time, end_time, state, task_tracker]   
         curr_task = HadoopTask.new(task["taskId"],
              task["startTime"], task["finishTime"],
              task["state"], task["taskTracker"])
         
         #figure out if curr_task was a suspect
         if curr_task.id =~ /(.*)(_[1-9][0-9]*)$/ then
           curr_task.is_retry = true
           #also mark the 0th attempt of this task as being suspect
           @suspects << $1 + "_0"
         end

         #catch bad timestamps
         if curr_task.start != 0 and curr_task.duration > 0 then
           #save the maps and reducers
           if is_reduce : @reduces << curr_task
           else @maps << curr_task
           end
         else
           @bad_tasks << curr_task
         end
       end
     end
   end
  end

  def set_suspects
    tasks.each do |x|
       x.is_suspect = !@suspects.find{|tid| tid == x.id}.nil?
    end
  end

  def set_stats
    [@maps, @reduces].each_with_index do |tasks,i|
      tasks.each do |task|
        if i == 0 : @map_stats << task.duration
        else @reduce_stats << task.duration
        end
      end
    end
  end
end


class HadoopTask
  attr_reader :start, :finish, :state, :state, :id, :tracker, :is_retry, :is_suspect
  attr_writer :is_retry, :is_suspect

  def initialize (task_id, start_time, end_time, state, tracker, is_retry=false)
    @id = task_id
    @start = start_time
    @finish = end_time
    @state = state
    @tracker = tracker
    @is_retry = is_retry
    @is_suspect = false
  end

  def start;   @start/1000.0   end 
  def finish;  @finish/1000.0  end 

  def duration
    (@finish-@start > 0 and @finish-@start < 100000000) ? (@finish-@start)/1000.0 : 0
  end 

  def is_success 
    @state == "SUCCEEDED" or @state == "RUNNING" ? true : false 
  end
  
  # there are 5 types of tasks to distinguish between:
  #   0) not-suspected, i.e. succeeded on first try --- :good
  #   1) suspected, succeeded --- :suspect_good
  #   2) suspected, killed --- :suspect_killed
  #   3) speculative, succeeded --- :spec_good
  #   4) speculative, killed --- :spec_killed
  def spec_state
    if is_success and not @is_retry and not @is_suspect
      return :good
    end
    
    if is_success and not @is_retry and @is_suspect then
      return :suspect_good
    end
   
    if not is_success and not @is_retry then
      return :suspect_killed
    end
  
    if is_success and @is_retry then
      return :spec_good
    end
 
    if not is_success and @is_retry then
      return :spec_killed
    end
  end
 
  def to_s
    out = ""
    instance_variables.each do |x|
      out << x + ": " + eval(x).to_s
    end
    return out
  end
end

