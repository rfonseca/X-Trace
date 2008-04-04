def html_table(headers, values)
  html = "<table>\n"
  html << "<tr>" << headers.map{|x| "<th>#{x}</th>"}.join << "</tr>\n"
  values.each do |value|
    html << "<tr>" << value.map{|x| "<td>#{x}</td>"}.join << "</tr>\n"
  end
  html << "</table>\n"
  return html
end

def vertical_table(table)
  html = "<table>\n"
  table.each{|k,v| html << "<tr><th>#{k}</th><td>#{v}</td></tr>\n"}
  html << "</table>\n"
  return html
end

def duration_table(times, labels)
  table = []
  duration = times.last - times.first
  labels.each_with_index do |label, index|
    dif = times[index+1] - times[index]
    dif_pct = dif / duration * 100.0
    table << [label, "%.1fs (%.1f%%)" % [dif, dif_pct]]
  end
  return vertical_table(table)
end
