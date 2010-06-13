/*
* Copyright (c) 2005,2006,2007 The Regents of the University of California.
* All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the University of California, nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

#ifndef _XTR_REPORT_CTX_H
#define _XTR_REPORT_CTX_H 1

#include <netinet/in.h>
#include "XtrConstants.h"
#include "XtrOption.h" 


/** A Reporter is a static class used to send reports to the
 *  reporting daemon. It must be initialized with the static method
 *  Reporter::init(), and then any number of reports can be sent
 *  using the static method(s) Reporter::sendReport.
 *  The reports are sent via UDP to a local socket. The port is by 
 *  default XTR_RPT_UDP_PORT = 7831, but this setting is overridden if
 *  the environment variable XTR_DAEMON_PORT.
 */
namespace xtr {
    
class Reporter
{
public:
    /** Sets up any state required to have the class ready to send
     *  reports.*/
    static void init();

    /** Cleans up any state set up at init */
    static void stop();

    /** Sends an X-Trace report to the Ctx's destination.
     *  Currently this means a local UDP socket running on port 7831.
     *  @param msg a null-terminated C-style string. It will not be changed
     *             and can be freed/overwritten after the call.
     *  @param severity Severity of the event being reported
     *  @param severityThreshold Severity threshold of the metadata in question.
     *         Unless this is equal to OptionSeverity::_UNSET, it overrides the
     *         internal severity_thresh of this reporter.
     *  @return XTR_SUCCESS if message is sent ok
     *          XTR_FAIL_SEVERITY if message fails severity check
     *          XTR_FAIL if reporter not initialized or there is some other
     *                   error.
     */
    static xtr_result sendReport(const char *msg, 
                                 u_int8_t severity = OptionSeverity::DEFAULT, 
                                 u_int8_t severityThreshold = OptionSeverity::_UNSET
                                );

    /** Returns true if the reporter is initialized and 
     *  severity <= effective severity threshold.
     *
     *  This function will serve as a HINT as to whether the reporter
     *  would report an event of *severity* for a task with
     *  severityThreshold. While the comparison is the same as the
     *  one made during actual reporting, the result may change if the
     *  internal severity threshold changes in between calls.
     *
     *  The effective severity threshold is given by:
     *  (severityThreshold == OptionSeverity::_UNSET)?severityThreshold:severity_thresh
     *  where severityThreshold is the argument (likely from the current metadata),
     *  
     *  @param severity Severity of the event being reported
     *  @param severityThreshold Severity threshold of the metadata in question.
     *         Unless this is equal to OptionSeverity::_UNSET, it overrides the
     *         internal severity_thresh of this reporter.
     *  @return XTR_SUCCESS if will report
     *          XTR_FAIL_SEVERITY if it won't report because of severity
     *          XTR_FAIL if reporter not initialized
     */
    static xtr_result willReport(u_int8_t severity, 
                                 u_int8_t severityThreshold = OptionSeverity::_UNSET);



    /** Severity settings */
    
    /** Sets the minimum severity messages must have to be reported.
     *  @param severity A message with numeric severity M will only be reported by the
     *                  reporting context with severity threshold R if M <= R.
     *                  Two special values are allowed: 
     *                  - OptionSeverity::_ALL will report all messages
     *                  - OptionSeverity::_NONE will report no messages 
     *  @see  Option.h for the definitions of OptionSeverity levels
     */
    static xtr_result setSeverityThreshold(u_int8_t severity);

    /** Returns the currently set severity threshold */
    static u_int8_t getSeverityThreshold();

    /** Not implemented. Sends out XtrReport objects */
    //static xtr_result sendReport(const XtrReport&); 
private:
    static int initialized;
    static int report_socket;
    static struct sockaddr_in dstaddr;
    static u_int8_t severity_thresh;
};

}; //namespace xtr

#endif

