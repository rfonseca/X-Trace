#!/usr/bin/env ruby

require 'rubygems'
require 'cgi'
require 'erb'
require 'json'
require 'logger'
require 'net/http'
require 'socket'
require 'statistics2.rb'
require 'lib/gnuplot.rb'
require 'lib/htmlutils.rb'
require 'lib/reports.rb'
require 'lib/stats.rb'

DEBUG = true
PROGRESS_LOG_DIR = "/Users/matei/workspace/hadoop-apache/logs/progress"

cgi = CGI.new('html4')
SERVER_URL = "http://#{cgi.server_name}:#{cgi.server_port}"
io = open("hadoop-tasks.erb")
template = ERB.new(io.read)
io.close
cgi.out { template.result(binding) }

